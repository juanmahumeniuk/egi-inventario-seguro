import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Persona, 
  EspecificacionesEspecificas, 
  MaquinaConAsignaciones,
  Aula,
  formatAulaName
} from './types';
import ItuLogo from './components/ItuLogo';
import LoginScreen from './components/LoginScreen';
import MaquinaDetailDrawer from './components/MaquinaDetailDrawer';
import MaquinaCrudModal from './components/MaquinaCrudModal';
import { maquinaService } from './services/maquinaService';
import { authService } from './services/authService';
import { 
  Search, 
  Plus, 
  Edit, 
  Trash2, 
  LogOut, 
  FileCheck, 
  RefreshCw, 
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
  Sparkles,
  Calendar
} from 'lucide-react';

export default function App() {
  // Authentication State
  const [currentUser, setCurrentUser] = useState<string | null>(() => {
    return localStorage.getItem('itu_logged_user');
  });

  // DB States
  const [personas, setPersonas] = useState<Persona[]>([]);
  const [maquinasList, setMaquinasList] = useState<MaquinaConAsignaciones[]>([]);

  // DB Sync indicator / notification state / Loading indicators
  const [syncing, setSyncing] = useState(false);
  const [loadingData, setLoadingData] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  // UI Filtering & Load States
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedAula, setSelectedAula] = useState<string>('Todos');
  const [selectedCategory, setSelectedCategory] = useState<string>('Todas');
  
  // Simulated Loading in Blocks (Cargadas por bloque / Paginación de 5 en 5)
  const [currentPage, setCurrentPage] = useState(1);
  const [blockSize, setBlockSize] = useState(5); // Show in blocks of 5

  // Interactive Selection State (Drawer)
  const [selectedMaquinaId, setSelectedMaquinaId] = useState<number | null>(null);

  // Modal State
  const [showCrudModal, setShowCrudModal] = useState(false);
  const [editingMaquina, setEditingMaquina] = useState<MaquinaConAsignaciones | null>(null);

  // Custom Delete confirmation State
  const [maquinaIdToDelete, setMaquinaIdToDelete] = useState<number | null>(null);

  // Load data helper
  const loadData = async () => {
    if (!currentUser) return;
    setLoadingData(true);
    try {
      const [fetchedMaquinas, fetchedPersonas] = await Promise.all([
        maquinaService.getMaquinas(),
        maquinaService.getPersonas()
      ]);
      setMaquinasList(fetchedMaquinas);
      setPersonas(fetchedPersonas);
    } catch (err: any) {
      console.error(err);
      triggerSyncMessage('Error al cargar datos del servidor.');
    } finally {
      setLoadingData(false);
    }
  };

  // Initialize DB data from services if logged in
  useEffect(() => {
    if (currentUser) {
      loadData();
    }
  }, [currentUser]);

  const handleLogin = (username: string) => {
    setCurrentUser(username);
    localStorage.setItem('itu_logged_user', username);
    triggerSyncMessage('Sesión iniciada. Sistema sincronizado.');
  };

  const handleLogout = () => {
    authService.logout();
    setCurrentUser(null);
  };

  const triggerSyncMessage = (msg: string) => {
    setStatusMessage(msg);
    setTimeout(() => {
      setStatusMessage(null);
    }, 5000);
  };

  // Delete Action
  const handleDeleteConfirm = async () => {
    if (maquinaIdToDelete === null) return;

    setSyncing(true);
    try {
      await maquinaService.deleteMaquina(maquinaIdToDelete);
      
      if (selectedMaquinaId === maquinaIdToDelete) {
        setSelectedMaquinaId(null);
      }
      
      triggerSyncMessage(`Máquina #${maquinaIdToDelete} dada de baja.`);
      await loadData();
    } catch (err: any) {
      console.error(err);
      triggerSyncMessage('Error al eliminar la máquina.');
    } finally {
      setSyncing(false);
      setMaquinaIdToDelete(null);
    }
  };

  // Save / CRUD Action
  const handleSaveMaquina = async (
    maquinaData: {
      numero_mesa: number;
      aula: Aula;
      fecha_mantenimiento: string;
    },
    specsData: Omit<EspecificacionesEspecificas, 'maquina_id'>,
    assignedPersonaIds: { id: number; fecha_asignado: string }[]
  ) => {
    setSyncing(true);
    try {
      if (editingMaquina) {
        // ===== UPDATE OPERATION =====
        await maquinaService.updateMaquina(
          editingMaquina.id,
          maquinaData,
          specsData,
          assignedPersonaIds
        );
        triggerSyncMessage(`Máquina #${editingMaquina.id} actualizada correctamente.`);
      } else {
        // ===== CREATE OPERATION =====
        const newId = await maquinaService.createMaquina(
          maquinaData,
          specsData,
          assignedPersonaIds
        );
        triggerSyncMessage(`Máquina #${newId} creada correctamente.`);
      }

      await loadData();
    } catch (err: any) {
      console.error(err);
      triggerSyncMessage('Error al guardar los cambios de la máquina.');
    } finally {
      setSyncing(false);
      setShowCrudModal(false);
      setEditingMaquina(null);
    }
  };

  // Filter logic using the state fetched from service
  const filteredMaquinasList = maquinasList.filter((maquina) => {
    // 1. Search Query (filters by ID, location, or assigned persons)
    const matchesSearch = 
      maquina.id.toString().includes(searchQuery) ||
      formatAulaName(maquina.aula).toLowerCase().includes(searchQuery.toLowerCase()) ||
      maquina.especificaciones.modelo.toLowerCase().includes(searchQuery.toLowerCase()) ||
      maquina.especificaciones.fabricante.toLowerCase().includes(searchQuery.toLowerCase()) ||
      maquina.asignaciones.some((asig) => {
        const fullname = `${asig.persona.nombre} ${asig.persona.apellido}`.toLowerCase();
        return fullname.includes(searchQuery.toLowerCase());
      });

    // 2. Filter by Aula
    const matchesAula = selectedAula === 'Todos' || maquina.aula === selectedAula;

    // 3. Filter by assigned persona categories
    const matchesCategory = selectedCategory === 'Todas' || maquina.asignaciones.some((asig) => asig.persona.categoria === selectedCategory);

    return matchesSearch && matchesAula && matchesCategory;
  });

  // BLOCK LOADING PAGINATION CALCULATIONS
  // Since we load inside blocks, determine subset based on current block index
  const totalItems = filteredMaquinasList.length;
  const totalPages = Math.ceil(totalItems / blockSize) || 1;
  
  // Safe page navigation
  const activePage = currentPage > totalPages ? totalPages : currentPage;
  
  const startIndex = (activePage - 1) * blockSize;
  const endIndex = startIndex + blockSize;
  const pageMaquinasBlock = filteredMaquinasList.slice(startIndex, endIndex);

  // Active selected machine detailed specs
  const selectedMaquina = maquinasList.find((m) => m.id === selectedMaquinaId) || null;

  // Format today's date in ITU format
  const formattedDate = new Date().toLocaleDateString('es-AR', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  // Guard: require login screen first
  if (!currentUser) {
    return <LoginScreen onLoginSuccess={handleLogin} />;
  }

  return (
    <div id="app-root-layout" className="min-h-screen bg-slate-50 flex flex-col text-slate-800">

      
      {/* HEADER (30% Teal background accent & border) */}
      <header id="main-header" className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-xs">
        <div className="max-w-[1600px] mx-auto px-4 sm:px-6 py-4 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          
          {/* Logo & Slogan */}
          <div className="flex items-center gap-4">
            <ItuLogo size="md" />
            <div className="hidden sm:block h-6 w-px bg-slate-200" />
            <div className="hidden sm:block">
              <span className="text-slate-400 font-mono text-[9px] block tracking-widest uppercase">Sistema Centralizado</span>
              <span className="font-extrabold text-xs text-[#064E3B] tracking-wide">Control de Inventario de Laboratorios</span>
            </div>
          </div>

          {/* Connected databases indicators & Welcome user menu */}
          <div className="flex flex-wrap items-center gap-3 sm:gap-5 self-end md:self-center">
            
            {/* User credentials & Logout */}
            <div className="flex items-center gap-2.5 bg-emerald-50 border border-emerald-200 px-3.5 py-1.5 rounded-full">
              <div className="w-7 h-7 rounded-full bg-[#064E3B] text-white flex items-center justify-center font-bold text-xs shadow-inner">
                {currentUser.substring(0, 2).toUpperCase()}
              </div>
              <div className="text-left">
                <span className="text-[10px] text-slate-400 block font-mono">USUARIO LOGUEADO</span>
                <span className="text-xs font-bold text-slate-700 block">{currentUser}</span>
              </div>
              <button
                onClick={handleLogout}
                className="ml-2 p-1.5 text-slate-400 hover:text-[#DC2626] focus:outline-none transition-colors rounded-lg hover:bg-slate-200/50"
                title="Cerrar sesión"
              >
                <LogOut size={16} />
              </button>
            </div>
          </div>

        </div>
      </header>

      {/* WORKSPACE AREA */}
      <main className="flex-1 w-full max-w-[1600px] mx-auto px-4 sm:px-6 py-6 flex flex-col gap-6">
        
        {/* WELCOME BAR & SYSTEM TIME */}
        <div id="greeting-bar" className="bg-white rounded-2xl p-5 border border-slate-200/60 shadow-xs flex flex-col md:flex-row justify-between items-start md:items-center gap-3 relative overflow-hidden">
          <div className="absolute top-0 left-0 bottom-0 w-1.5 bg-[#064E3B]" />
          <div>
            <h2 className="text-xl font-black text-slate-800 tracking-tight">
              Hola, <span className="text-[#064E3B]">{currentUser}</span>
            </h2>
            <p className="text-slate-400 text-xs mt-0.5 capitalize flex items-center gap-1.5">
              <Calendar size={13} />
              {formattedDate}
            </p>
          </div>

          {/* Sync indicator */}
          <div className="flex items-center gap-2.5">
            {syncing ? (
              <span className="flex items-center gap-1.5 text-xs text-slate-500 font-medium font-mono">
                <RefreshCw size={14} className="animate-spin text-[#064E3B]" />
                Actualizando datos...
              </span>
            ) : statusMessage ? (
              <div className="px-3.5 py-1.5 bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs rounded-lg font-semibold flex items-center gap-1.5 animate-fadeIn">
                <Sparkles size={13} className="text-emerald-600" />
                {statusMessage}
              </div>
            ) : (
              <span className="text-[11px] text-slate-400 font-semibold font-mono flex items-center gap-1">
                <FileCheck size={14} className="text-emerald-500" />
                Sistema Sincronizado y Actualizado
              </span>
            )}
          </div>
        </div>

        {/* CONTROLS ROW - Search, filters, ADD button */}
        <div id="control-panel" className="bg-white rounded-2xl p-4 border border-slate-200/60 shadow-xs flex flex-col xl:flex-row items-stretch xl:items-center gap-4 justify-between">
          
          {/* Left part: Search inputs & Filter select dropdowns */}
          <div className="flex flex-col sm:flex-row flex-1 items-stretch sm:items-center gap-3">
            
            {/* Search Input */}
            <div className="relative flex-1">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400" size={17} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setCurrentPage(1); // reset to page 1 on search
                }}
                placeholder="Buscar Máquina por #id, aula, fabricante, asignado..."
                className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-[#064E3B] focus:border-[#064E3B] transition-all font-medium text-slate-700"
              />
            </div>

            {/* Aula Filter */}
            <div className="flex items-center gap-1.5 shrink-0">
              <SlidersHorizontal size={14} className="text-slate-400 hidden lg:block" />
              <span className="text-xs font-bold text-slate-500 hidden lg:block uppercase tracking-wider">Aula:</span>
              <select
                value={selectedAula}
                onChange={(e) => {
                  setSelectedAula(e.target.value);
                  setCurrentPage(1);
                }}
                className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-600 focus:ring-1 focus:ring-[#064E3B]"
              >
                <option value="Todos">Todas las Aulas</option>
                <option value={Aula.AULA_1y2}>Aula 1 y 2</option>
                <option value={Aula.LABORATORIO_SO}>Laboratorio SO</option>
                <option value={Aula.LABORATORIO_REDES}>Laboratorio Redes</option>
                <option value={Aula.AULA_4}>Aula 4</option>
              </select>
            </div>

            {/* Category Filter */}
            <div className="flex items-center gap-1.5 shrink-0">
              <span className="text-xs font-bold text-slate-500 hidden lg:block uppercase tracking-wider">Asignación:</span>
              <select
                value={selectedCategory}
                onChange={(e) => {
                  setSelectedCategory(e.target.value);
                  setCurrentPage(1);
                }}
                className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold text-slate-600 focus:ring-1 focus:ring-[#064E3B]"
              >
                <option value="Todas">Cualquier Categoría</option>
                <option value="Docente">Docentes</option>
                <option value="Alumno">Alumnos</option>
                <option value="Responsable Técnico">Responsables Técnicos</option>
              </select>
            </div>

          </div>

          {/* Right part: Red Accent primary action button representing 10% CTA */}
          <button
            onClick={() => {
              setEditingMaquina(null);
              setShowCrudModal(true);
            }}
            className="px-5 py-2.5 bg-[#DC2626] hover:bg-[#b91c1c] text-white font-extrabold text-xs rounded-xl uppercase tracking-wider transition-all flex items-center justify-center gap-2 shadow-md hover:shadow-lg hover:-translate-y-0.5"
          >
            <Plus size={16} />
            Cargar Máquina
          </button>
        </div>

        {/* WORKSPACE SPLIT VIEW */}
        <div id="split-layout" className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          
          {/* LEFT: Desktop data list (spanning 12 units unless drawer is open) */}
          <div className={`space-y-4 lg:col-span-12 ${selectedMaquinaId ? 'lg:col-span-7 xl:col-span-8' : ''} transition-all duration-300`}>
            
            {/* Table Card Container */}
            <div className="bg-white rounded-2xl border border-slate-200/60 shadow-xs overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left border-collapse table-auto text-sm">
                  
                  {/* Table Header */}
                  <thead>
                    <tr className="bg-slate-50 border-b border-slate-200 font-bold text-slate-500 text-xs uppercase tracking-wider">
                      <th className="px-5 py-4 w-16">#ID</th>
                      <th className="px-5 py-4">Ubicación</th>
                      <th className="px-5 py-4 text-center">Nro Banco</th>
                      <th className="px-5 py-4">Mantenimiento</th>
                      <th className="px-5 py-4">Personas Asignadas</th>
                      <th className="px-5 py-4 text-center">Acciones</th>
                    </tr>
                  </thead>

                  {/* Table Body */}
                  <tbody className="divide-y divide-slate-100 font-semibold text-slate-700">
                    {loadingData ? (
                      // Render skeletons
                      [...Array(blockSize)].map((_, idx) => (
                        <tr key={`skeleton-${idx}`} className="animate-pulse">
                          <td className="px-5 py-4 w-16">
                            <div className="h-6 w-10 bg-slate-200 rounded" />
                          </td>
                          <td className="px-5 py-4">
                            <div className="h-5 w-32 bg-slate-200 rounded" />
                          </td>
                          <td className="px-5 py-4 text-center">
                            <div className="h-8 w-8 bg-slate-200 rounded-full mx-auto" />
                          </td>
                          <td className="px-5 py-4">
                            <div className="h-4 w-20 bg-slate-200 rounded" />
                          </td>
                          <td className="px-5 py-4">
                            <div className="space-y-2">
                              <div className="h-4 w-24 bg-slate-200 rounded" />
                              <div className="h-4 w-16 bg-slate-200 rounded" />
                            </div>
                          </td>
                          <td className="px-5 py-4 text-center">
                            <div className="h-8 w-16 bg-slate-200 rounded mx-auto" />
                          </td>
                        </tr>
                      ))
                    ) : (
                      pageMaquinasBlock.map((maquina) => {
                        const isHighlighted = selectedMaquinaId === maquina.id;
                        return (
                          <tr 
                            key={maquina.id}
                            className={`cursor-pointer transition-all ${
                              isHighlighted 
                                ? 'bg-emerald-50 hover:bg-emerald-50' 
                                : 'hover:bg-slate-50/75'
                            }`}
                            onClick={() => setSelectedMaquinaId(isHighlighted ? null : maquina.id)}
                          >
                            {/* ID clickable element */}
                            <td className="px-5 py-4 font-mono text-slate-400">
                              <span className="inline-block px-2.5 py-1 bg-slate-100 text-slate-600 rounded-lg text-xs font-extrabold">
                                #{maquina.id}
                              </span>
                            </td>

                            {/* Location */}
                            <td className="px-5 py-4">
                              <span className="text-slate-900 block font-bold">
                                {formatAulaName(maquina.aula)}
                              </span>
                            </td>

                            {/* Number of Mesa/banco */}
                            <td className="px-5 py-4 text-center font-mono">
                              <span className="inline-block w-8 h-8 rounded-full bg-slate-50 border border-slate-200 flex items-center justify-center font-bold text-slate-700 mx-auto">
                                {maquina.numero_mesa}
                              </span>
                            </td>

                            {/* Maintenance Date */}
                            <td className="px-5 py-4 text-slate-500 font-mono text-xs">
                              {maquina.fecha_mantenimiento}
                            </td>

                            {/* Complex Assignments (Highlighting Many-to-Many connection as in wireframe) */}
                            <td className="px-5 py-4">
                              {maquina.asignaciones.length === 0 ? (
                                <span className="text-slate-400 font-normal italic text-xs">Sin asignar actualmente</span>
                              ) : (
                                <div className="space-y-1.5 max-w-[280px]">
                                  {maquina.asignaciones.map(({ persona }) => (
                                    <div key={persona.id} className="flex items-center gap-1.5">
                                      <span className="text-xs font-bold text-slate-800 leading-none">
                                        {persona.nombre} {persona.apellido[0]}.
                                      </span>
                                      <span className={`text-[9px] font-mono px-1.5 py-0.5 rounded-full font-bold uppercase ${
                                        persona.categoria === 'Docente' 
                                          ? 'bg-blue-50 text-blue-700 border-blue-200' 
                                          : persona.categoria === 'Responsable Técnico'
                                          ? 'bg-purple-50 text-purple-700 border-purple-200'
                                          : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                      }`}>
                                        {persona.categoria === 'Responsable Técnico' ? 'Técnico' : persona.categoria}
                                      </span>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </td>

                            {/* Action elements */}
                            <td className="px-5 py-4 text-center" onClick={(e) => e.stopPropagation()}>
                              <div className="flex items-center justify-center gap-1.5">
                                <button
                                  onClick={() => {
                                    setEditingMaquina(maquina);
                                    setShowCrudModal(true);
                                  }}
                                  className="p-1.5 hover:bg-slate-200 rounded-lg text-slate-500 hover:text-[#064E3B] transition-colors"
                                  title="Editar máquina"
                                >
                                  <Edit size={16} />
                                </button>
                                <button
                                  onClick={() => setMaquinaIdToDelete(maquina.id)}
                                  className="p-1.5 hover:bg-slate-200 rounded-lg text-slate-500 hover:text-[#DC2626] transition-colors"
                                  title="Eliminar máquina"
                                >
                                  <Trash2 size={16} />
                                </button>
                              </div>
                            </td>

                          </tr>
                        );
                      })
                    )}


                    {totalItems === 0 && (
                      <tr>
                        <td colSpan={6} className="px-5 py-12 text-center">
                          <div className="max-w-xs mx-auto space-y-2">
                            <span className="text-slate-400 text-sm font-semibold block">No se encontraron resultados</span>
                            <p className="text-slate-400 text-xs">
                              Prueba ajustando la consulta de búsqueda o los filtros de categorías.
                            </p>
                          </div>
                        </td>
                      </tr>
                    )}
                  </tbody>

                </table>
              </div>

              {/* TABLE BLOCKS PAGINATION (Cargar por bloques) */}
              {totalItems > 0 && (
                <div className="bg-slate-50 border-t border-slate-100 px-5 py-4 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
                  <div className="text-slate-400 font-semibold font-mono">
                    Mostrando del <span className="text-slate-700">{startIndex + 1}</span> al <span className="text-slate-700">{Math.min(endIndex, totalItems)}</span> de <span className="text-slate-700">{totalItems}</span> computadoras cargadas por bloque
                  </div>

                  <div className="flex items-center gap-3">
                    {/* Block Size Selector */}
                    <div className="flex items-center gap-1">
                      <span className="text-slate-400 font-semibold text-[10px] uppercase">Bloque:</span>
                      <select
                        value={blockSize}
                        onChange={(e) => {
                          setBlockSize(parseInt(e.target.value, 10));
                          setCurrentPage(1);
                        }}
                        className="py-1 px-1.5 bg-white border border-slate-200 rounded text-slate-600 font-mono font-bold focus:ring-0 text-[11px]"
                      >
                        <option value={3}>3 PCs</option>
                        <option value={5}>5 PCs</option>
                        <option value={10}>10 PCs</option>
                      </select>
                    </div>

                    {/* Pagination Buttons */}
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                        disabled={activePage === 1}
                        className="p-1 px-2 border border-slate-200 bg-white hover:bg-slate-100 rounded text-slate-600 disabled:opacity-40 transition-all cursor-pointer font-bold flex items-center"
                      >
                        <ChevronLeft size={14} />
                      </button>
                      <span className="text-slate-500 font-bold font-mono px-2">
                        Página {activePage} de {totalPages}
                      </span>
                      <button
                        onClick={() => setCurrentPage((p) => Math.min(p + 1, totalPages))}
                        disabled={activePage === totalPages}
                        className="p-1 px-2 border border-slate-200 bg-white hover:bg-slate-100 rounded text-slate-600 disabled:opacity-40 transition-all cursor-pointer font-bold flex items-center"
                      >
                        <ChevronRight size={14} />
                      </button>
                    </div>
                  </div>
                </div>
              )}

            </div>



          </div>

          {/* RIGHT: Desktop Detail Sidebar Panel */}
          {selectedMaquina && (
            <div className="hidden lg:block lg:col-span-5 xl:col-span-4 lg:self-start lg:sticky lg:top-24 lg:h-[calc(100vh-140px)] transition-all duration-300">
              <AnimatePresence mode="wait">
                <motion.div
                  key="desktop-drawer-panel"
                  initial={{ opacity: 0, scale: 0.98 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.98 }}
                  transition={{ duration: 0.2 }}
                  className="h-full bg-white rounded-2xl border border-slate-200/60 shadow-xs flex flex-col overflow-hidden"
                >
                  <MaquinaDetailDrawer
                    maquina={selectedMaquina}
                    onClose={() => setSelectedMaquinaId(null)}
                    onEdit={() => {
                      setEditingMaquina(selectedMaquina);
                      setShowCrudModal(true);
                    }}
                  />
                </motion.div>
              </AnimatePresence>
            </div>
          )}

        </div>

      </main>

      {/* MOBILE FLOAT DETAIL OVERLAY DRAWER */}
      <AnimatePresence>
        {selectedMaquina && (
          <div className="lg:hidden">
            {/* Mobile Backdrop Overlay (darkens background beneath) */}
            <motion.div
              key="mobile-drawer-backdrop"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={() => setSelectedMaquinaId(null)}
              className="fixed inset-0 z-40 bg-slate-900/60 backdrop-blur-xs"
            />

            {/* Animated Mobile Drawer Panel */}
            <motion.div
              key="mobile-drawer-panel"
              initial={{ x: '100%' }}
              animate={{ x: 0 }}
              exit={{ x: '100%' }}
              transition={{ type: 'spring', damping: 26, stiffness: 220 }}
              className="fixed top-0 right-0 bottom-0 z-50 w-full max-w-md h-full bg-white shadow-2xl flex flex-col"
            >
              <MaquinaDetailDrawer
                maquina={selectedMaquina}
                onClose={() => setSelectedMaquinaId(null)}
                onEdit={() => {
                  setEditingMaquina(selectedMaquina);
                  setShowCrudModal(true);
                }}
              />
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* CRUD MODAL */}
      {showCrudModal && (
        <MaquinaCrudModal
          maquina={editingMaquina}
          personas={personas}
          onClose={() => {
            setShowCrudModal(false);
            setEditingMaquina(null);
          }}
          onSave={handleSaveMaquina}
        />
      )}

      {/* DELETE CONFIRM DIALOG */}
      {maquinaIdToDelete !== null && (
        <div className="fixed inset-0 z-50 overflow-y-auto flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-100 space-y-4 text-center">
            <span className="w-12 h-12 rounded-full bg-red-100 text-[#DC2626] flex items-center justify-center mx-auto text-xl font-bold">
              !
            </span>
            <div className="space-y-1.5">
              <h4 className="font-black text-slate-800 text-lg">¿Eliminar Computadora?</h4>
              <p className="text-slate-400 text-xs">
                Esta acción dará de baja definitiva a la PC #{maquinaIdToDelete} del sistema y eliminará todas sus asignaciones activas. Esta operación no se puede deshacer.
              </p>
            </div>
            <div className="flex gap-2.5 pt-2">
              <button
                onClick={() => setMaquinaIdToDelete(null)}
                className="flex-1 py-2 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs rounded-lg uppercase tracking-wider transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleDeleteConfirm}
                className="flex-1 py-2 px-4 bg-[#DC2626] hover:bg-[#b91c1c] text-white font-bold text-xs rounded-lg uppercase tracking-wider transition-colors"
              >
                Confirmar baja
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
