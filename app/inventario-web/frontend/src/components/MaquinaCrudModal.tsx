import React, { useState, useEffect } from 'react';
import { Persona, MaquinaConAsignaciones, EspecificacionesEspecificas, CategoriaPersona, Aula } from '../types';
import { X, Save, Plus, Database, Cpu, Monitor, UserCheck, Calendar, Info } from 'lucide-react';
import { maquinaService } from '../services/maquinaService';
interface MaquinaCrudModalProps {
  maquina: MaquinaConAsignaciones | null; // Null means adding a new one
  personas: Persona[];
  onSave: (
    maquinaData: {
      numero_mesa: number;
      aula: Aula;
      fecha_mantenimiento: string;
    },
    specsData: Omit<EspecificacionesEspecificas, 'maquina_id'>,
    assignedPersonaIds: { id: number; fecha_asignado: string }[]
  ) => void;
  onClose: () => void;
}

export default function MaquinaCrudModal({ maquina, personas, onSave, onClose }: MaquinaCrudModalProps) {
  // Primary characteristics
  const [numeroMesa, setNumeroMesa] = useState(5);
  const [aula, setAula] = useState<Aula>(Aula.AULA_1y2);
  const [fechaMantenimiento, setFechaMantenimiento] = useState('2026-06-01');

  // Specs characteristics (MongoDB JSON)
  const [fabricante, setFabricante] = useState('Dell');
  const [modelo, setModelo] = useState('OptiPlex 7090');
  const [tipo, setTipo] = useState<'desktop' | 'laptop' | 'all-in-one'>('desktop');
  const [cpu, setCpu] = useState('Intel Core i7-11700');
  const [ramGb, setRamGb] = useState(16);
  const [discoTipo, setDiscoTipo] = useState<'SSD' | 'HDD'>('SSD');
  const [discoCapacidad, setDiscoCapacidad] = useState(512);
  const [sistemaOperativo, setSistemaOperativo] = useState('Windows 11 Pro');
  const [monitor, setMonitor] = useState('Dell P2422H 24"');
  const [mouse, setMouse] = useState('Dell MS116');
  const [teclado, setTeclado] = useState('Dell KB216');

  // Many-to-Many assignments selection
  // Map of persona_id -> { selected: boolean, fecha_asignado: string }
  const [assignments, setAssignments] = useState<Record<number, { selected: boolean; fecha_asignado: string }>>({});

  // Help fields for creating a NEW Persona inline!
  const [showAddPersona, setShowAddPersona] = useState(false);
  const [newPersonaNombre, setNewPersonaNombre] = useState('');
  const [newPersonaApellido, setNewPersonaApellido] = useState('');
  const [newPersonaCategoria, setNewPersonaCategoria] = useState<CategoriaPersona>('Alumno');
  const [localPersonas, setLocalPersonas] = useState<Persona[]>(personas);

  // Sync state if editing a machine
  useEffect(() => {
    if (maquina) {
      setNumeroMesa(maquina.numero_mesa);
      setAula(maquina.aula);
      setFechaMantenimiento(maquina.fecha_mantenimiento);

      setFabricante(maquina.especificaciones.fabricante);
      setModelo(maquina.especificaciones.modelo);
      setTipo(maquina.especificaciones.tipo || 'desktop');
      setCpu(maquina.especificaciones.cpu);
      setRamGb(maquina.especificaciones.ram_gb);
      setDiscoTipo(maquina.especificaciones.disco.tipo);
      setDiscoCapacidad(maquina.especificaciones.disco.capacidad_gb);
      setSistemaOperativo(maquina.especificaciones.sistema_operativo);
      setMonitor(maquina.especificaciones.perifericos.monitor);
      setMouse(maquina.especificaciones.perifericos.mouse);
      setTeclado(maquina.especificaciones.perifericos.teclado);

      // Map current assignments
      const currentAssigned: Record<number, { selected: boolean; fecha_asignado: string }> = {};
      
      // Initialize with false for all personas
      localPersonas.forEach((p) => {
        currentAssigned[p.id] = { selected: false, fecha_asignado: new Date().toISOString().split('T')[0] };
      });

      // Overlay actual assignments
      maquina.asignaciones.forEach((asig) => {
        currentAssigned[asig.persona.id] = {
          selected: true,
          fecha_asignado: asig.fecha_asignado,
        };
      });

      setAssignments(currentAssigned);
    } else {
      // Default initial assignments for new machine
      const initAssigned: Record<number, { selected: boolean; fecha_asignado: string }> = {};
      localPersonas.forEach((p) => {
        initAssigned[p.id] = { selected: false, fecha_asignado: new Date().toISOString().split('T')[0] };
      });
      setAssignments(initAssigned);
    }
  }, [maquina, localPersonas]);

  // Handle adding a new Person dynamically to database representation
  const handleAddNewPersonaSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newPersonaNombre.trim() || !newPersonaApellido.trim()) return;

    try {
      const newPersona = await maquinaService.createPersona({
        nombre: newPersonaNombre.trim(),
        apellido: newPersonaApellido.trim(),
        categoria: newPersonaCategoria,
      });

      const updated = [...localPersonas, newPersona];
      setLocalPersonas(updated);
      
      // Auto-select this newly created person
      setAssignments((prev) => ({
        ...prev,
        [newPersona.id]: {
          selected: true,
          fecha_asignado: new Date().toISOString().split('T')[0],
        },
      }));

      // Reset fields
      setNewPersonaNombre('');
      setNewPersonaApellido('');
      setNewPersonaCategoria('Alumno');
      setShowAddPersona(false);
    } catch (err) {
      console.error("Error creating persona", err);
      alert("Error al crear persona. Verifique la conexión con el servidor.");
    }
  };

  const handleAssignmentToggle = (personaId: number) => {
    setAssignments((prev) => {
      const current = prev[personaId] || { selected: false, fecha_asignado: new Date().toISOString().split('T')[0] };
      return {
        ...prev,
        [personaId]: {
          ...current,
          selected: !current.selected,
        },
      };
    });
  };

  const handleAssignmentDateChange = (personaId: number, dateStr: string) => {
    setAssignments((prev) => {
      const current = prev[personaId] || { selected: false, fecha_asignado: new Date().toISOString().split('T')[0] };
      return {
        ...prev,
        [personaId]: {
          ...current,
          fecha_asignado: dateStr,
        },
      };
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Prepare list of assigned personas
    const selectedAssignments = (Object.entries(assignments) as [string, { selected: boolean; fecha_asignado: string }][])
      .filter(([_, value]) => value.selected)
      .map(([idStr, value]) => ({
        id: parseInt(idStr, 10),
        fecha_asignado: value.fecha_asignado,
      }));

    onSave(
      {
        numero_mesa: numeroMesa,
        aula: aula,
        fecha_mantenimiento: fechaMantenimiento,
      },
      {
        fabricante,
        modelo,
        tipo,
        cpu,
        ram_gb: ramGb,
        disco: { tipo: discoTipo, capacidad_gb: discoCapacidad },
        sistema_operativo: sistemaOperativo,
        perifericos: { monitor, mouse, teclado },
      },
      selectedAssignments
    );
  };

  return (
    <div id="modal-container" className="fixed inset-0 z-50 overflow-y-auto flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
      
      <div 
        id="modal-body" 
        className="relative bg-white rounded-2xl w-full max-w-4xl shadow-2xl border border-slate-100 flex flex-col max-h-[90vh]"
      >
        {/* Header */}
        <div className="bg-[#064E3B] text-white px-6 py-4 rounded-t-2xl flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-2">
            <Database size={20} className="text-white/80" />
            <h3 className="font-bold text-lg">
              {maquina ? `Editar Computadora #${maquina.id}` : 'Cargar Nueva Computadora'}
            </h3>
          </div>
          <button 
            onClick={onClose}
            className="p-1.5 hover:bg-white/10 rounded-lg text-white/95 hover:text-white transition-all"
            title="Cerrar modal"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-8">
          
          {/* Main Info */}
          <section className="space-y-4">
            <h4 className="text-[#064E3B] text-xs font-bold uppercase tracking-wider border-b border-slate-200 pb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-[#064E3B]" />
              Información Básica del Banco
            </h4>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Aula / Laboratorio</label>
                <select
                  value={aula}
                  onChange={(e) => setAula(e.target.value as Aula)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-bold focus:ring-1 focus:ring-[#064E3B]"
                >
                  <option value={Aula.AULA_1y2}>Aula 1 y 2</option>
                  <option value={Aula.LABORATORIO_SO}>Laboratorio Sistemas Operativos</option>
                  <option value={Aula.LABORATORIO_REDES}>Laboratorio Redes</option>
                  <option value={Aula.AULA_4}>Aula 4</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Número de Mesa / Banco</label>
                <input
                  type="number"
                  required
                  min={1}
                  value={numeroMesa}
                  onChange={(e) => setNumeroMesa(parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Último Mantenimiento</label>
                <input
                  type="date"
                  required
                  value={fechaMantenimiento}
                  onChange={(e) => setFechaMantenimiento(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                />
              </div>
            </div>
          </section>

          {/* Technical Specs */}
          <section className="space-y-4">
            <h4 className="text-[#064E3B] text-xs font-bold uppercase tracking-wider border-b border-slate-200 pb-2 flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-[#064E3B]" />
              Especificaciones de Hardware
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Fabricante</label>
                <input
                  type="text"
                  required
                  value={fabricante}
                  onChange={(e) => setFabricante(e.target.value)}
                  placeholder="Dell, HP, Lenovo, Apple"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Modelo del Gabinete / PC</label>
                <input
                  type="text"
                  required
                  value={modelo}
                  onChange={(e) => setModelo(e.target.value)}
                  placeholder="Ej. OptiPlex 7090"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Arquitectura / Tipo</label>
                <select
                  value={tipo}
                  onChange={(e) => setTipo(e.target.value as 'desktop' | 'laptop' | 'all-in-one')}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                >
                  <option value="desktop">Escritorio (Desktop)</option>
                  <option value="laptop">Portátil (Laptop)</option>
                  <option value="all-in-one">Todo en Uno (All-in-One)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Unidad de Procesamiento (CPU)</label>
                <input
                  type="text"
                  required
                  value={cpu}
                  onChange={(e) => setCpu(e.target.value)}
                  placeholder="Intel Core i7-11700 / AMD Ryzen 7"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#0d7671]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Capacidad RAM (GB)</label>
                <input
                  type="number"
                  required
                  min={4}
                  value={ramGb}
                  onChange={(e) => setRamGb(parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Sistema Operativo Principal</label>
                <input
                  type="text"
                  required
                  value={sistemaOperativo}
                  onChange={(e) => setSistemaOperativo(e.target.value)}
                  placeholder="Windows 11 Pro, Ubuntu 22.04"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                />
              </div>

              {/* Nested Storage Specs */}
              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Tipo de Disco</label>
                <select
                  value={discoTipo}
                  onChange={(e) => setDiscoTipo(e.target.value as 'SSD' | 'HDD')}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                >
                  <option value="SSD">SSD (Sólido - Ultra Rápido)</option>
                  <option value="HDD">HDD (Mecánico - Alta Capacidad)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-500 mb-1">Capacidad Disco (GB)</label>
                <input
                  type="number"
                  required
                  min={120}
                  step={4}
                  value={discoCapacidad}
                  onChange={(e) => setDiscoCapacidad(parseInt(e.target.value, 10))}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-sm text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                />
              </div>
            </div>

            <div className="bg-slate-50 p-4 rounded-xl border border-slate-200/50 space-y-3">
              <span className="text-xs font-bold text-slate-600 block flex items-center gap-1">
                <Monitor size={13} className="text-[#064E3B]" />
                Asignación de Periféricos de Escritorio
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-[10px] font-semibold text-slate-500 uppercase">Modelo Monitor</label>
                  <input
                    type="text"
                    required
                    value={monitor}
                    onChange={(e) => setMonitor(e.target.value)}
                    placeholder='Dell P2422H 24"'
                    className="w-full px-2.5 py-1.5 bg-white border border-slate-200 rounded-md text-xs text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-semibold text-slate-500 uppercase">Modelo Mouse</label>
                  <input
                    type="text"
                    required
                    value={mouse}
                    onChange={(e) => setMouse(e.target.value)}
                    placeholder="Dell MS116"
                    className="w-full px-2.5 py-1.5 bg-white border border-slate-200 rounded-md text-xs text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                  />
                </div>
                <div>
                  <label className="block text-[10px] font-semibold text-slate-500 uppercase">Modelo Teclado</label>
                  <input
                    type="text"
                    required
                    value={teclado}
                    onChange={(e) => setTeclado(e.target.value)}
                    placeholder="Dell KB216"
                    className="w-full px-2.5 py-1.5 bg-white border border-slate-200 rounded-md text-xs text-slate-800 font-medium focus:ring-1 focus:ring-[#064E3B]"
                  />
                </div>
              </div>
            </div>
          </section>

          {/* Many-to-Many Relational Assignments */}
          <section className="space-y-4">
            <div className="flex justify-between items-center border-b border-slate-200 pb-2">
              <h4 className="text-[#064E3B] text-xs font-bold uppercase tracking-wider flex items-center gap-2">
                <UserCheck size={14} className="text-[#DC2626]" />
                Asignación de Personas Responsables
              </h4>
              <button
                type="button"
                onClick={() => setShowAddPersona(!showAddPersona)}
                className="text-xs bg-[#064E3B] hover:bg-[#043e2e] text-white px-2.5 py-1 rounded-md font-semibold transition-colors flex items-center gap-1 shadow-xs"
              >
                <Plus size={13} />
                Agregar Nueva Persona
              </button>
            </div>

            {/* Sub-form to add a new person dynamically */}
            {showAddPersona && (
              <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 space-y-4 animate-fadeIn">
                <div className="flex justify-between items-center mb-1">
                  <h5 className="text-xs font-bold text-slate-700">Registrar Nueva Persona</h5>
                  <button 
                    type="button" 
                    onClick={() => setShowAddPersona(false)}
                    className="text-slate-400 hover:text-slate-600 text-xs"
                  >
                    Cancelar
                  </button>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="block text-[10px] font-semibold text-slate-500">Nombre</label>
                    <input
                      type="text"
                      value={newPersonaNombre}
                      onChange={(e) => setNewPersonaNombre(e.target.value)}
                      placeholder="Ej. Gonzalo"
                      className="w-full px-3 py-1.5 bg-white border border-slate-200 rounded-md text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-semibold text-slate-500">Apellido</label>
                    <input
                      type="text"
                      value={newPersonaApellido}
                      onChange={(e) => setNewPersonaApellido(e.target.value)}
                      placeholder="Ej. Morales"
                      className="w-full px-3 py-1.5 bg-white border border-slate-200 rounded-md text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-semibold text-slate-500">Categoría</label>
                    <select
                      value={newPersonaCategoria}
                      onChange={(e) => setNewPersonaCategoria(e.target.value as CategoriaPersona)}
                      className="w-full px-3 py-1.5 bg-white border border-slate-200 rounded-md text-xs"
                    >
                      <option value="Docente">Docente</option>
                      <option value="Alumno">Alumno</option>
                      <option value="Responsable Técnico">Responsable Técnico</option>
                    </select>
                  </div>
                </div>
                <div className="flex justify-end">
                  <button
                    type="button"
                    onClick={handleAddNewPersonaSubmit}
                    className="px-3 py-1.5 bg-slate-800 hover:bg-slate-900 text-white rounded-md text-xs font-bold"
                  >
                    Guardar Persona
                  </button>
                </div>
              </div>
            )}

            <div className="space-y-2 max-h-56 overflow-y-auto pr-2">
              <span className="text-[10px] text-slate-400 font-mono block">Selecciona las personas que usan/tienen asignada esta PC:</span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {localPersonas.map((persona) => {
                  const state = assignments[persona.id] || { selected: false, fecha_asignado: new Date().toISOString().split('T')[0] };
                  return (
                    <div 
                      key={persona.id}
                      onClick={() => handleAssignmentToggle(persona.id)}
                      className={`p-3 border rounded-xl cursor-pointer transition-all flex flex-col justify-between ${
                        state.selected 
                          ? 'bg-emerald-50 border-[#064E3B] ring-1 ring-[#064E3B]/45' 
                          : 'bg-white border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <input
                            type="checkbox"
                            checked={state.selected}
                            readOnly
                            className="rounded text-[#064E3B] focus:ring-[#064E3B] border-slate-300 pointer-events-none"
                          />
                          <span className="font-semibold text-xs text-slate-800 leading-tight">
                            {persona.nombre} {persona.apellido}
                          </span>
                        </div>
                        <span className={`text-[9px] px-2 py-0.5 rounded-full font-bold uppercase ${
                          persona.categoria === 'Docente' 
                            ? 'bg-blue-50 text-blue-700' 
                            : persona.categoria === 'Responsable Técnico'
                            ? 'bg-purple-50 text-purple-700'
                            : 'bg-emerald-50 text-emerald-700'
                        }`}>
                          {persona.categoria}
                        </span>
                      </div>

                      {state.selected && (
                        <div 
                          className="mt-2.5 pt-2 border-t border-slate-200/60 flex items-center gap-2"
                          onClick={(e) => e.stopPropagation()} // Stop toggle when picking date
                        >
                          <span className="text-[10px] text-slate-400 font-mono flex items-center gap-0.5 shrink-0">
                            <Calendar size={11} />
                            F. Asignado:
                          </span>
                          <input
                            type="date"
                            value={state.fecha_asignado}
                            onChange={(e) => handleAssignmentDateChange(persona.id, e.target.value)}
                            className="w-full px-2 py-1 text-[11px] bg-white border border-slate-200 rounded focus:ring-0.5 focus:ring-[#064E3B]"
                          />
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </section>


        </form>

        {/* Footer actions */}
        <div id="modal-footer" className="bg-slate-50 p-4 border-t border-slate-100 rounded-b-2xl shrink-0 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-[#064E3B] bg-slate-100 hover:bg-slate-200 font-bold text-xs rounded-lg uppercase tracking-wider transition-colors"
          >
            Cancelar
          </button>

          {/* RED Button as 10% CTA */}
          <button
            type="button"
            onClick={handleSubmit}
            className="px-5 py-2.5 bg-[#DC2626] hover:bg-[#b91c1c] text-white font-bold text-xs rounded-lg uppercase tracking-wider transition-colors flex items-center gap-1.5 shadow-md"
          >
            <Save size={14} />
            {maquina ? 'Guardar Cambios' : 'Ingresar Equipo'}
          </button>
        </div>

      </div>
    </div>
  );
}
