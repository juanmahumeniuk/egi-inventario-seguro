import { Persona, Maquina, PersonaMaquina, EspecificacionesEspecificas, MaquinaConAsignaciones, Aula } from './types';

// Seed Initial Data
export const INITIAL_PERSONAS: Persona[] = [
  { id: 101, nombre: 'Gonzalo', apellido: 'Morales', categoria: 'Alumno' },
  { id: 102, nombre: 'Martín', apellido: 'Valenzuela', categoria: 'Docente' },
  { id: 103, nombre: 'Juan', apellido: 'Pérez', categoria: 'Responsable Técnico' },

  { id: 104, nombre: 'Laura', apellido: 'Gómez', categoria: 'Docente' },
  { id: 105, nombre: 'Lucas', apellido: 'Díaz', categoria: 'Alumno' },
  { id: 106, nombre: 'Sofía', apellido: 'Rodríguez', categoria: 'Alumno' },
  { id: 107, nombre: 'Carlos', apellido: 'Benítez', categoria: 'Responsable Técnico' },
  { id: 108, nombre: 'Patricia', apellido: 'Sosa', categoria: 'Docente' },
];

export const INITIAL_MAQUINAS: Maquina[] = [
  { id: 1, numero_mesa: 5, fecha_mantenimiento: '2026-06-01', aula: Aula.AULA_1y2 },
  { id: 2, numero_mesa: 8, fecha_mantenimiento: '2026-05-15', aula: Aula.LABORATORIO_SO },
  { id: 3, numero_mesa: 3, fecha_mantenimiento: '2026-05-20', aula: Aula.LABORATORIO_REDES },
  { id: 4, numero_mesa: 12, fecha_mantenimiento: '2026-06-02', aula: Aula.AULA_4 },
  { id: 42, numero_mesa: 15, fecha_mantenimiento: '2026-04-10', aula: Aula.AULA_1y2 },
];

export const INITIAL_PERSONA_MAQUINAS: PersonaMaquina[] = [
  // Máquina 1: Gonzalo Morales (Alumno), Martín Valenzuela (Docente), Juan Pérez (Técnico)
  { persona_id: 101, maquina_id: 1, fecha_asignado: '2026-06-01' },
  { persona_id: 102, maquina_id: 1, fecha_asignado: '2026-06-01' },
  { persona_id: 103, maquina_id: 1, fecha_asignado: '2026-06-01' },

  // Máquina 2: Laura Gómez (Docente), Sofía Rodríguez (Alumno)
  { persona_id: 104, maquina_id: 2, fecha_asignado: '2026-05-15' },
  { persona_id: 106, maquina_id: 2, fecha_asignado: '2026-05-15' },

  // Máquina 3: Lucas Díaz (Alumno), Martín Valenzuela (Docente)
  { persona_id: 105, maquina_id: 3, fecha_asignado: '2026-05-20' },
  { persona_id: 102, maquina_id: 3, fecha_asignado: '2026-05-20' },

  // Máquina 4: Patricia Sosa (Docente), Sofía Rodríguez (Alumno), Carlos Benítez (Técnico)
  { persona_id: 108, maquina_id: 4, fecha_asignado: '2026-06-02' },
  { persona_id: 106, maquina_id: 4, fecha_asignado: '2026-06-02' },
  { persona_id: 107, maquina_id: 4, fecha_asignado: '2026-06-02' },

  // Máquina 42: Carlos Benítez (Técnico), Gonzalo Morales (Alumno)
  { persona_id: 107, maquina_id: 42, fecha_asignado: '2026-04-10' },
  { persona_id: 101, maquina_id: 42, fecha_asignado: '2026-04-10' },
];

export const INITIAL_ESPECIFICACIONES: EspecificacionesEspecificas[] = [
  {
    _id: { $oid: '665f1a2b3c4d5e6f7a8b9c0d' },
    maquina_id: 1,
    fabricante: 'Dell',
    modelo: 'OptiPlex 7090',
    tipo: 'desktop',
    cpu: 'Intel Core i5-11500',
    ram_gb: 16,
    disco: { tipo: 'SSD', capacidad_gb: 512 },
    sistema_operativo: 'Windows 11 Pro',
    perifericos: {
      monitor: 'Dell P2422H 24"',
      mouse: 'Dell MS116',
      teclado: 'Dell KB216'
    }
  },
  {
    _id: { $oid: '665f1a2b3c4d5e6f7a8b9c0e' },
    maquina_id: 2,
    fabricante: 'HP',
    modelo: 'ProDesk 400 G6',
    tipo: 'desktop',
    cpu: 'Intel Core i5-10500',
    ram_gb: 8,
    disco: { tipo: 'SSD', capacidad_gb: 256 },
    sistema_operativo: 'Ubuntu Linux 22.04 LTS',
    perifericos: {
      monitor: 'HP P22h G4 21.5"',
      mouse: 'HP USB Wired Mouse',
      teclado: 'HP USB Wired Keyboard'
    }
  },
  {
    _id: { $oid: '665f1a2b3c4d5e6f7a8b9c0f' },
    maquina_id: 3,
    fabricante: 'Lenovo',
    modelo: 'ThinkCentre M70q',
    tipo: 'desktop',
    cpu: 'AMD Ryzen 5 5600GE',
    ram_gb: 16,
    disco: { tipo: 'SSD', capacidad_gb: 480 },
    sistema_operativo: 'Windows 10 Pro',
    perifericos: {
      monitor: 'Lenovo ThinkVision T24i-20',
      mouse: 'Lenovo Essential USB Mouse',
      teclado: 'Lenovo Essential USB Keyboard'
    }
  },
  {
    _id: { $oid: '665f1a2b3c4d5e6f7a8b9c10' },
    maquina_id: 4,
    fabricante: 'Asus',
    modelo: 'ExpertCenter D5',
    tipo: 'desktop',
    cpu: 'Intel Core i7-11700',
    ram_gb: 32,
    disco: { tipo: 'SSD', capacidad_gb: 1024 },
    sistema_operativo: 'Windows 11 Pro',
    perifericos: {
      monitor: 'Asus VY249HE 23.8"',
      mouse: 'Asus WT200 Wireless',
      teclado: 'Asus Wired Keyboard'
    }
  },
  {
    _id: { $oid: '665f1a2b3c4d5e6f7a8b9c11' },
    maquina_id: 42,
    fabricante: 'Dell',
    modelo: 'OptiPlex 7090',
    tipo: 'desktop',
    cpu: 'Intel Core i7-11700',
    ram_gb: 16,
    disco: { tipo: 'SSD', capacidad_gb: 512 },
    sistema_operativo: 'Windows 10 Pro',
    perifericos: {
      monitor: 'Dell P2422H 24"',
      mouse: 'Dell MS116',
      teclado: 'Dell KB216'
    }
  }
];

// Helper to load state from localStorage or seed
export function getSavedData() {
  const personas = localStorage.getItem('itu_personas');
  const maquinas = localStorage.getItem('itu_maquinas');
  const personaMaquinas = localStorage.getItem('itu_persona_maquinas');
  const especificaciones = localStorage.getItem('itu_especificaciones');

  let parsedMaquinas: Maquina[] = maquinas ? JSON.parse(maquinas) : INITIAL_MAQUINAS;

  // Gracefully migrate any old formats to standard enum values
  parsedMaquinas = parsedMaquinas.map((m: any) => {
    // If we have "laboratorio" field or the aula is not one of our enum values
    if (m.laboratorio !== undefined || !Object.values(Aula).includes(m.aula)) {
      let migratedAula = Aula.AULA_1y2;
      const labUpper = String(m.laboratorio || '').toUpperCase();
      const aulaUpper = String(m.aula || '').toUpperCase();

      if (labUpper.includes('REDES') || aulaUpper.includes('REDES') || aulaUpper.includes('3')) {
        migratedAula = Aula.LABORATORIO_REDES;
      } else if (labUpper.includes('OS') || labUpper.includes('SO') || aulaUpper.includes('OS') || aulaUpper.includes('SO')) {
        migratedAula = Aula.LABORATORIO_SO;
      } else if (aulaUpper.includes('4') || labUpper.includes('PROGRA')) {
        migratedAula = Aula.AULA_4;
      }
      return {
        id: m.id,
        numero_mesa: m.numero_mesa,
        fecha_mantenimiento: m.fecha_mantenimiento,
        aula: migratedAula
      };
    }
    return m;
  });

  return {
    personas: personas ? JSON.parse(personas) : INITIAL_PERSONAS,
    maquinas: parsedMaquinas,
    personaMaquinas: personaMaquinas ? JSON.parse(personaMaquinas) : INITIAL_PERSONA_MAQUINAS,
    especificaciones: especificaciones ? JSON.parse(especificaciones) : INITIAL_ESPECIFICACIONES,
  };
}

export function saveData(data: {
  personas: Persona[];
  maquinas: Maquina[];
  personaMaquinas: PersonaMaquina[];
  especificaciones: EspecificacionesEspecificas[];
}) {
  localStorage.setItem('itu_personas', JSON.stringify(data.personas));
  localStorage.setItem('itu_maquinas', JSON.stringify(data.maquinas));
  localStorage.setItem('itu_persona_maquinas', JSON.stringify(data.personaMaquinas));
  localStorage.setItem('itu_especificaciones', JSON.stringify(data.especificaciones));
}

// Build denormalized structure for full display in the client
export function denormalizeMaquinas(
  maquinas: Maquina[],
  personas: Persona[],
  personaMaquinas: PersonaMaquina[],
  especificaciones: EspecificacionesEspecificas[]
): MaquinaConAsignaciones[] {
  return maquinas.map((maquina) => {
    // Find specs in Mongo JSON
    const spec = especificaciones.find((s) => s.maquina_id === maquina.id) || {
      maquina_id: maquina.id,
      fabricante: 'Genérica',
      modelo: 'Modelo Standard',
      tipo: 'desktop',
      cpu: 'Intel Core i5',
      ram_gb: 8,
      disco: { tipo: 'SSD', capacidad_gb: 256 },
      sistema_operativo: 'Windows 10',
      perifericos: { monitor: 'Genérico 21"', mouse: 'Genérico USB', teclado: 'Genérico USB' }
    };

    // Find relations in Relational Join
    const relations = personaMaquinas.filter((pm) => pm.maquina_id === maquina.id);
    const asignaciones = relations.map((rel) => {
      const persona = personas.find((p) => p.id === rel.persona_id) || {
        id: rel.persona_id,
        nombre: 'Usuario',
        apellido: 'No Encontrado',
        categoria: 'Alumno' as const
      };
      return {
        persona,
        fecha_asignado: rel.fecha_asignado,
      };
    });

    return {
      ...maquina,
      especificaciones: spec,
      asignaciones,
    };
  });
}
