// Central API Client - Toggleable between mock and real REST APIs
import { getSavedData, saveData } from '../data';
import { Persona, Maquina, PersonaMaquina, EspecificacionesEspecificas, Aula } from '../types';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

// Delay helper for mock responses
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export interface ApiResponse<T> {
  data?: T;
  error?: string;
  status: number;
}

// Fetch helper with JWT interceptor
export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('itu_auth_token');
  const headers = new Headers(options.headers || {});
  
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  
  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `HTTP error! status: ${response.status}`);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json() as Promise<T>;
}

// Mock Implementation Database state in localStorage
// This simulates the behavior of the backend coordinate flow:
// - Query SQL Server (personas, maquinas, personaMaquinas)
// - If machine exists, query MongoDB (especificaciones)
export const mockDb = {
  async getPersonas(): Promise<Persona[]> {
    await delay(300);
    const data = getSavedData();
    return data.personas;
  },

  async getMaquinas(): Promise<Maquina[]> {
    await delay(400);
    const data = getSavedData();
    // Simulate SQL server query for all machines
    return data.maquinas;
  },

  async getEspecificaciones(): Promise<EspecificacionesEspecificas[]> {
    await delay(300);
    const data = getSavedData();
    // Simulate MongoDB query for specs
    return data.especificaciones;
  },

  async getPersonaMaquinas(): Promise<PersonaMaquina[]> {
    await delay(200);
    const data = getSavedData();
    // Simulate SQL relation table query
    return data.personaMaquinas;
  },

  // Simulates obtaining a single unified machine.
  // This simulates the backend's rule: Query SQL first, then MongoDB if found.
  async getMaquinaById(id: number) {
    await delay(400); // Simulate SQL search
    const data = getSavedData();
    const maquina = data.maquinas.find(m => m.id === id);
    
    if (!maquina) {
      throw new Error(`Máquina con ID ${id} no encontrada en SQL Server (ubicacion-db).`);
    }

    // Machine exists in SQL Server. Now query MongoDB for specs:
    await delay(250); // Simulate Mongo query
    const spec = data.especificaciones.find(s => s.maquina_id === id);
    
    // Join details
    const relations = data.personaMaquinas.filter(pm => pm.maquina_id === id);
    const asignaciones = relations.map(rel => {
      const persona = data.personas.find(p => p.id === rel.persona_id) || {
        id: rel.persona_id,
        nombre: 'Usuario',
        apellido: 'Desconocido',
        categoria: 'Alumno' as const
      };
      return {
        persona,
        fecha_asignado: rel.fecha_assigned || rel.fecha_asignado
      };
    });

    return {
      ...maquina,
      especificaciones: spec || {
        maquina_id: id,
        fabricante: 'Genérica',
        modelo: 'Modelo Standard',
        tipo: 'desktop' as const,
        cpu: 'Intel Core i5',
        ram_gb: 8,
        disco: { tipo: 'SSD' as const, capacidad_gb: 256 },
        sistema_operativo: 'Windows 10',
        perifericos: { monitor: 'Genérico', mouse: 'Genérico', teclado: 'Genérico' }
      },
      asignaciones
    };
  },

  async createMaquina(
    maquinaData: { numero_mesa: number; aula: Aula; fecha_mantenimiento: string },
    specsData: Omit<EspecificacionesEspecificas, 'maquina_id'>,
    assignedPersonaIds: { id: number; fecha_asignado: string }[]
  ) {
    await delay(500); // Simulate SQL + Mongo transactions
    const data = getSavedData();
    
    const nextId = Math.max(...data.maquinas.map(m => m.id), 0) + 1;
    
    // 1. Insert in SQL Server Mock
    const newMaquina: Maquina = {
      id: nextId,
      ...maquinaData
    };
    const updatedMaquinas = [newMaquina, ...data.maquinas];

    // 2. Insert in MongoDB Mock
    const newSpecs: EspecificacionesEspecificas = {
      _id: { $oid: `665f1a2b3c4d5e${Math.random().toString(16).substring(2, 10)}` },
      maquina_id: nextId,
      ...specsData
    };
    const updatedSpecs = [newSpecs, ...data.especificaciones];

    // 3. Insert N:N Relations in SQL Server Mock
    const newRelations = assignedPersonaIds.map(item => ({
      persona_id: item.id,
      maquina_id: nextId,
      fecha_asignado: item.fecha_asignado
    }));
    const updatedPM = [...newRelations, ...data.personaMaquinas];

    saveData({
      personas: data.personas,
      maquinas: updatedMaquinas,
      personaMaquinas: updatedPM,
      especificaciones: updatedSpecs
    });

    return nextId;
  },

  async updateMaquina(
    id: number,
    maquinaData: { numero_mesa: number; aula: Aula; fecha_mantenimiento: string },
    specsData: Omit<EspecificacionesEspecificas, 'maquina_id'>,
    assignedPersonaIds: { id: number; fecha_asignado: string }[]
  ) {
    await delay(600); // Simulate SQL + Mongo update transactions
    const data = getSavedData();

    // 1. Check SQL Server
    const exists = data.maquinas.some(m => m.id === id);
    if (!exists) {
      throw new Error(`No se puede actualizar: Máquina con ID ${id} no existe en SQL Server.`);
    }

    // 2. Update SQL Server Maquina table
    const updatedMaquinas = data.maquinas.map(m => 
      m.id === id ? { ...m, ...maquinaData } : m
    );

    // 3. Update MongoDB specifications document
    const updatedSpecs = data.especificaciones.map(spec => 
      spec.maquina_id === id 
        ? { ...spec, ...specsData, maquina_id: id } 
        : spec
    );

    // 4. Update SQL Server PersonaMaquina relation records (Delete & Insert)
    const filteredPM = data.personaMaquinas.filter(pm => pm.maquina_id !== id);
    const newRelations = assignedPersonaIds.map(item => ({
      persona_id: item.id,
      maquina_id: id,
      fecha_asignado: item.fecha_asignado
    }));
    const updatedPM = [...filteredPM, ...newRelations];

    saveData({
      personas: data.personas,
      maquinas: updatedMaquinas,
      personaMaquinas: updatedPM,
      especificaciones: updatedSpecs
    });

    return id;
  },

  async deleteMaquina(id: number) {
    await delay(500); // Simulate SQL + Mongo delete transaction
    const data = getSavedData();

    // 1. Check SQL Server
    const exists = data.maquinas.some(m => m.id === id);
    if (!exists) {
      throw new Error(`No se puede eliminar: Máquina con ID ${id} no existe en SQL Server.`);
    }

    // Delete SQL Maquina table row and PersonaMaquina relations
    const updatedMaquinas = data.maquinas.filter(m => m.id !== id);
    const updatedPM = data.personaMaquinas.filter(pm => pm.maquina_id !== id);

    // Delete Mongo specifications
    const updatedSpecs = data.especificaciones.filter(spec => spec.maquina_id !== id);

    saveData({
      personas: data.personas,
      maquinas: updatedMaquinas,
      personaMaquinas: updatedPM,
      especificaciones: updatedSpecs
    });

    return id;
  }
};

export { USE_MOCK };
