export type CategoriaPersona = 'Docente' | 'Alumno' | 'Responsable Técnico';

export interface Persona {
  id: number;
  nombre: string;
  apellido: string;
  categoria: CategoriaPersona;
}

export enum Aula {
  AULA_1y2 = 'AULA_1y2',
  LABORATORIO_SO = 'LABORATORIO_SO',
  LABORATORIO_REDES = 'LABORATORIO_REDES',
  AULA_4 = 'AULA_4'
}

export function formatAulaName(aula: Aula | string): string {
  switch (aula) {
    case Aula.AULA_1y2:
      return 'Aula 1 y 2';
    case Aula.LABORATORIO_SO:
      return 'Laboratorio Sistemas Operativos';
    case Aula.LABORATORIO_REDES:
      return 'Laboratorio Redes';
    case Aula.AULA_4:
      return 'Aula 4';
    default:
      return String(aula);
  }
}

export interface Maquina {
  id: number;
  numero_mesa: number;
  fecha_mantenimiento: string;
  aula: Aula;
}

export interface PersonaMaquina {
  persona_id: number;
  maquina_id: number;
  fecha_asignado: string;
}

export interface DiscoSpec {
  tipo: 'SSD' | 'HDD';
  capacidad_gb: number;
}

export interface PerifericosSpec {
  monitor: string;
  mouse: string;
  teclado: string;
}

export interface EspecificacionesEspecificas {
  _id?: { $oid: string } | string;
  maquina_id: number;
  fabricante: string;
  modelo: string;
  tipo: 'desktop' | 'laptop' | 'all-in-one';
  cpu: string;
  ram_gb: number;
  disco: DiscoSpec;
  sistema_operativo: string;
  perifericos: PerifericosSpec;
}

// Helper types for the unified view
export interface MaquinaConAsignaciones extends Maquina {
  especificaciones: EspecificacionesEspecificas;
  asignaciones: {
    persona: Persona;
    fecha_asignado: string;
  }[];
}
