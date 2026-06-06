import { apiFetch, USE_MOCK, mockDb } from './api';
import { MaquinaConAsignaciones, Persona, Aula, EspecificacionesEspecificas } from '../types';
import { denormalizeMaquinas } from '../data';

export const maquinaService = {
  // Returns all machines, unified with their specifications and assignments.
  // Backend Flow:
  // 1. Fetch machines from SQL Server.
  // 2. Fetch assignments (PersonaMaquina + Persona) from SQL Server.
  // 3. Fetch specs from MongoDB for each machine.
  // 4. Return unified JSON payload.
  async getMaquinas(): Promise<MaquinaConAsignaciones[]> {
    if (USE_MOCK) {
      const maquinas = await mockDb.getMaquinas();
      const personas = await mockDb.getPersonas();
      const pm = await mockDb.getPersonaMaquinas();
      const specs = await mockDb.getEspecificaciones();
      return denormalizeMaquinas(maquinas, personas, pm, specs);
    }

    return apiFetch<MaquinaConAsignaciones[]>('/maquinas');
  },

  // Returns all registered personas (used to populate selectors in forms).
  // Backend Flow: Query SQL Server (Persona table).
  async getPersonas(): Promise<Persona[]> {
    if (USE_MOCK) {
      return mockDb.getPersonas();
    }

    return apiFetch<Persona[]>('/personas');
  },

  // Returns details of a specific machine.
  // Backend Flow:
  // 1. Query SQL Server for machine ID. If not found, return 404.
  // 2. Query SQL Server for assignments of this machine.
  // 3. Query MongoDB for specifications of this machine.
  // 4. Return combined JSON.
  async getMaquinaById(id: number): Promise<MaquinaConAsignaciones> {
    if (USE_MOCK) {
      return mockDb.getMaquinaById(id);
    }

    return apiFetch<MaquinaConAsignaciones>(`/maquinas/${id}`);
  },

  // Creates a machine.
  // Backend Flow:
  // 1. Start SQL Server transaction. Insert Maquina and return ID.
  // 2. Insert PersonaMaquina relations in SQL.
  // 3. Insert specifications in MongoDB referencing the new ID.
  // 4. Commit transactions.
  async createMaquina(
    maquinaData: { numero_mesa: number; aula: Aula; fecha_mantenimiento: string },
    specsData: Omit<EspecificacionesEspecificas, 'maquina_id'>,
    assignedPersonaIds: { id: number; fecha_asignado: string }[]
  ): Promise<number> {
    if (USE_MOCK) {
      return mockDb.createMaquina(maquinaData, specsData, assignedPersonaIds);
    }

    const payload = {
      ...maquinaData,
      especificaciones: specsData,
      asignaciones: assignedPersonaIds.map(p => ({
        personaId: p.id,
        fecha_asignado: p.fecha_asignado
      }))
    };

    const response = await apiFetch<{ id: number }>('/maquinas', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    return response.id;
  },

  // Updates an existing machine.
  // Backend Flow:
  // 1. Check if machine exists in SQL Server. If not, error.
  // 2. Update Maquina table in SQL Server.
  // 3. Delete existing relations and insert new ones in PersonaMaquina.
  // 4. Update specifications document in MongoDB where maquina_id matches.
  async updateMaquina(
    id: number,
    maquinaData: { numero_mesa: number; aula: Aula; fecha_mantenimiento: string },
    specsData: Omit<EspecificacionesEspecificas, 'maquina_id'>,
    assignedPersonaIds: { id: number; fecha_asignado: string }[]
  ): Promise<number> {
    if (USE_MOCK) {
      return mockDb.updateMaquina(id, maquinaData, specsData, assignedPersonaIds);
    }

    const payload = {
      ...maquinaData,
      especificaciones: specsData,
      asignaciones: assignedPersonaIds.map(p => ({
        personaId: p.id,
        fecha_asignado: p.fecha_asignado
      }))
    };

    const response = await apiFetch<{ id: number }>(`/maquinas/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    return response.id;
  },

  // Deletes a machine.
  // Backend Flow:
  // 1. Start SQL Server transaction. Delete PersonaMaquina assignments, then Maquina record.
  // 2. Delete Specifications document in MongoDB.
  // 3. Commit transactions.
  async deleteMaquina(id: number): Promise<number> {
    if (USE_MOCK) {
      return mockDb.deleteMaquina(id);
    }

    await apiFetch<void>(`/maquinas/${id}`, {
      method: 'DELETE'
    });
    return id;
  }
};
