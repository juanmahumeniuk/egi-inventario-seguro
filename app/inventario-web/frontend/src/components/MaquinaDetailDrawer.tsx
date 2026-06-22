import React from 'react';
import { MaquinaConAsignaciones, formatAulaName } from '../types';
import { X, UserCheck, Calendar, ShieldCheck, Cpu, Monitor, Keyboard, Mouse, Edit } from 'lucide-react';

interface MaquinaDetailDrawerProps {
  maquina: MaquinaConAsignaciones;
  onClose: () => void;
  onEdit: () => void;
}

export default function MaquinaDetailDrawer({ maquina, onClose, onEdit }: MaquinaDetailDrawerProps) {
  return (
    <div 
      id="detail-drawer" 
      className="bg-white rounded-none lg:rounded-2xl border-l lg:border border-slate-200 shadow-xl overflow-hidden flex flex-col h-full transition-all duration-300"
    >
      {/* Header */}
      <div className="bg-[#064E3B] text-white px-5 py-4 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Monitor size={18} className="text-white/80" />
          <h3 className="font-bold text-lg">Detalle Técnico de PC</h3>
        </div>
        <button 
          onClick={onClose} 
          className="p-1 hover:bg-white/10 rounded-lg text-white/90 hover:text-white transition-colors"
          title="Cerrar detalle"
        >
          <X size={20} />
        </button>
      </div>

      {/* Basic Stats Bar */}
      <div className="bg-slate-50 border-b border-slate-100 p-5 flex justify-between items-center">
        <div>
          <span className="text-slate-400 font-mono text-xs block uppercase">Identificador</span>
          <h4 className="text-xl font-extrabold text-[#064E3B]">Máquina #{maquina.id}</h4>
        </div>
        <div className="text-right">
          <span className="text-slate-400 text-xs block font-mono">Aula / Ubicación</span>
          <span className="inline-block px-2.5 py-1 bg-emerald-50 text-[#064E3B] border border-emerald-200 text-xs font-bold rounded-lg mt-1">
            {formatAulaName(maquina.aula)}
          </span>
        </div>
      </div>

      {/* Content Area */}
      <div className="flex-1 overflow-y-auto p-5 space-y-6">
        <div className="space-y-6">
          {/* Visual Specs Card */}
          <div>
            <div className="flex justify-between items-center mb-3">
              <h5 className="font-bold text-slate-700 text-sm tracking-wide uppercase flex items-center gap-1.5">
                <Cpu size={14} className="text-[#064E3B]" />
                Especificaciones de Hardware
              </h5>
            </div>

            <div className="bg-slate-50 rounded-xl p-4 border border-slate-200/60 text-sm space-y-3">
              <div className="grid grid-cols-2 gap-2 pb-2 border-b border-slate-200/50">
                <span className="text-slate-400">Fabricante:</span>
                <span className="font-bold text-slate-700 text-right">{maquina.especificaciones.fabricante}</span>
              </div>
              <div className="grid grid-cols-2 gap-2 pb-2 border-b border-slate-200/50">
                <span className="text-slate-400">Modelo:</span>
                <span className="font-bold text-slate-700 text-right">{maquina.especificaciones.modelo}</span>
              </div>
              <div className="grid grid-cols-2 gap-2 pb-2 border-b border-slate-200/50">
                <span className="text-slate-400">CPU:</span>
                <span className="font-semibold text-slate-700 text-right">{maquina.especificaciones.cpu || 'Intel Core i5'}</span>
              </div>
              <div className="grid grid-cols-2 gap-2 pb-2 border-b border-slate-200/50">
                <span className="text-slate-400">Memoria RAM:</span>
                <span className="font-semibold text-slate-700 text-right">{maquina.especificaciones.ram_gb} GB DDR4</span>
              </div>
              <div className="grid grid-cols-2 gap-2 pb-2 border-b border-slate-200/50">
                <span className="text-slate-400">Almacenamiento:</span>
                <span className="font-semibold text-slate-700 text-right">
                  {maquina.especificaciones.disco
                    ? `${maquina.especificaciones.disco.capacidad_gb} GB (${maquina.especificaciones.disco.tipo})`
                    : 'Sin datos'}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-2 pb-2 border-b border-slate-200/50">
                <span className="text-slate-400">Sistema Operativo:</span>
                <span className="font-semibold text-slate-700 text-right text-[#064E3B]">
                  {maquina.especificaciones.sistema_operativo}
                </span>
              </div>
            </div>
          </div>

          {/* Peripherals Specs */}
          <div>
            <h5 className="font-bold text-slate-700 text-sm tracking-wide uppercase flex items-center gap-1.5 mb-3">
              <Monitor size={14} className="text-[#064E3B]" />
              Periféricos e Integrados
            </h5>
            <div className="bg-slate-50 rounded-xl p-4 border border-slate-200/60 text-sm space-y-3">
              <div className="flex items-start gap-2.5 pb-2 border-b border-slate-200/50">
                <Monitor size={14} className="text-slate-400 mt-1" />
                <div className="flex-1">
                  <span className="text-xs text-slate-400 block">Monitor</span>
                  <span className="font-semibold text-slate-700">{maquina.especificaciones.perifericos?.monitor ?? 'Sin datos'}</span>
                </div>
              </div>
              <div className="flex items-start gap-2.5 pb-2 border-b border-slate-200/50">
                <Mouse size={14} className="text-slate-400 mt-1" />
                <div className="flex-1">
                  <span className="text-xs text-slate-400 block">Mouse</span>
                  <span className="font-semibold text-slate-700">{maquina.especificaciones.perifericos?.mouse ?? 'Sin datos'}</span>
                </div>
              </div>
              <div className="flex items-start gap-2.5">
                <Keyboard size={14} className="text-slate-400 mt-1" />
                <div className="flex-1">
                  <span className="text-xs text-slate-400 block">Teclado</span>
                  <span className="font-semibold text-slate-700">{maquina.especificaciones.perifericos?.teclado ?? 'Sin datos'}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Relational Assignments Area */}
          <div>
            <h5 className="font-bold text-slate-700 text-sm tracking-wide uppercase flex items-center gap-1.5 mb-3">
              <UserCheck size={14} className="text-[#DC2626]" />
              Asignaciones Activas
            </h5>
            {maquina.asignaciones.length === 0 ? (
              <div className="p-4 border-2 border-dashed border-slate-200 rounded-xl text-center text-xs text-slate-400">
                Sin personas asignadas actualmente a esta máquina.
              </div>
            ) : (
              <div className="space-y-3">
                {maquina.asignaciones.map(({ persona, fecha_asignado }) => (
                  <div 
                    key={persona.id} 
                    className="border border-slate-200 bg-white rounded-xl p-3 flex justify-between items-center hover:border-slate-300 transition-colors"
                  >
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="font-semibold text-slate-800 text-sm">
                          {persona.nombre} {persona.apellido}
                        </span>
                        <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                          persona.categoria === 'Docente' 
                            ? 'bg-blue-50 text-blue-700 border border-blue-200' 
                            : persona.categoria === 'Responsable Técnico'
                            ? 'bg-purple-50 text-purple-700 border border-purple-200'
                            : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        }`}>
                          {persona.categoria}
                        </span>
                      </div>
                      <span className="text-slate-400 text-xs font-mono block mt-1">ID Persona: {persona.id}</span>
                    </div>
                    <div className="text-right text-xs text-slate-400">
                      <span className="block italic text-[10px] text-slate-400 uppercase tracking-wider">Asignado</span>
                      <span className="font-medium flex items-center gap-1 mt-0.5 justify-end">
                        <Calendar size={12} />
                        {fecha_asignado}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Last Maintenance info */}
          <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 flex items-center gap-3">
            <ShieldCheck className="text-[#064E3B] shrink-0" size={24} />
            <div>
              <span className="text-xs font-semibold text-[#064E3B] uppercase tracking-wider block">Mantenimiento Preventivo</span>
              <p className="text-slate-600 text-xs mt-0.5 font-medium">
                Fecha del último mantenimiento registrado: <span className="font-bold underline">{maquina.fecha_mantenimiento}</span>
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Primary Red Trigger button representing 10% CTA */}
      <div className="bg-slate-50 border-t border-slate-100 p-4 shrink-0 flex gap-2">
        <button
          onClick={onEdit}
          className="flex-1 py-2.5 px-4 bg-[#DC2626] hover:bg-[#b91c1c] text-white font-bold text-xs rounded-lg uppercase tracking-wide transition-colors flex items-center justify-center gap-2 shadow-xs"
        >
          <Edit size={14} />
          Modificar Equipo
        </button>
      </div>
    </div>
  );
}
