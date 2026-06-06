import React, { useState } from 'react';
import ItuLogo from './ItuLogo';
import { authService } from '../services/authService';

interface LoginScreenProps {
  onLoginSuccess: (username: string) => void;
}

export default function LoginScreen({ onLoginSuccess }: LoginScreenProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!username.trim()) {
      setError('Por favor, ingresa tu usuario.');
      return;
    }
    if (!password) {
      setError('Por favor, ingresa tu contraseña.');
      return;
    }

    setIsLoading(true);

    try {
      const response = await authService.login(username, password);
      onLoginSuccess(response.username);
    } catch (err: any) {
      setError(err.message || 'Error de credenciales en LDAP.');
    } finally {
      setIsLoading(false);
    }
  };


  return (
    <div id="login-layout" className="min-h-screen w-full flex items-center justify-center bg-slate-50 px-4 py-8">
      <div 
        id="login-card" 
        className="w-full max-w-4xl bg-white rounded-2xl shadow-xl border border-slate-100 flex flex-col md:flex-row overflow-hidden transition-all duration-300 transform hover:shadow-2xl"
      >
        {/* Left Side - Brand Presentation */}
        <div 
          id="login-brand-side" 
          className="w-full md:w-1/2 bg-gradient-to-br from-slate-50 to-slate-100 p-8 md:p-12 flex flex-col justify-between items-center text-center relative border-b md:border-b-0 md:border-r border-slate-200"
        >
          {/* Top subtle decoration */}
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-[#064E3B]" />

          <div className="my-auto py-8">
            <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-slate-800 mb-6 font-sans">
              Gestor de Inventario
            </h1>
            
            <div className="transform scale-110 md:scale-125 my-8 flex items-center justify-center">
              <ItuLogo size="lg" />
            </div>
            
            <p className="text-slate-500 text-sm max-w-sm mx-auto mt-6 leading-relaxed">
              Infraestructura y Laboratorios de Informática del ITU
              (Instituto Tecnológico Universitario).
            </p>
          </div>

          <div className="text-xs text-slate-400 font-mono">
            SISTEMA CENTRALIZADO v2.4.0
          </div>
        </div>

        {/* Right Side - Form */}
        <div id="login-form-side" className="w-full md:w-1/2 p-8 md:p-12 flex flex-col justify-center">
          <div className="mb-6">
            <h2 id="form-title" className="text-2xl font-bold text-slate-800">
              Iniciar Sesión
            </h2>
            <p className="text-slate-400 text-sm mt-1">
              Ingresa tus credenciales autorizadas del ITU
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            {error && (
              <div className="p-3 bg-red-50 border-l-4 border-[#DC2626] text-red-700 text-xs rounded font-medium">
                {error}
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-2">
                Usuario / Correo
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Ej. admin o tu nombre"
                className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-lg text-slate-800 font-medium placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#064E3B] focus:border-[#064E3B] transition-all"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 uppercase tracking-wider mb-2">
                Contraseña
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-4 py-3 bg-slate-50 border border-slate-200 rounded-lg text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#064E3B] focus:border-[#064E3B] transition-all"
              />
            </div>

            <div className="flex items-center justify-between text-xs text-slate-500">
              <label className="flex items-center gap-2 cursor-pointer">
                <input 
                  type="checkbox" 
                  defaultChecked 
                  className="rounded text-[#064E3B] focus:ring-[#064E3B] border-slate-300"
                />
                <span>Recordarme en esta PC</span>
              </label>
              <span className="hover:text-emerald-800 cursor-pointer transition-colors">
                ¿Olvidaste tu contraseña?
              </span>
            </div>

            {/* Red button representing 10% CTA */}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3 px-4 bg-[#DC2626] hover:bg-[#b91c1c] text-white font-bold rounded-lg transition-colors shadow-md hover:shadow-lg flex items-center justify-center gap-2"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              ) : (
                'Ingresar al Sistema'
              )}
            </button>
          </form>

          {/* Quick instructions panel */}
          <div className="mt-8 pt-6 border-t border-slate-100 text-xs text-slate-400 leading-relaxed bg-slate-50 -mx-4 px-4 py-3 rounded-lg border">
            <span className="font-bold text-[#064E3B] block mb-1">💡 Acceso de Demostración</span>
            Ingresa cualquier usuario y contraseña para ingresar de manera simplificada (ej. <code className="bg-slate-200 text-slate-700 px-1 py-0.5 rounded">admin</code> / <code className="bg-slate-200 text-slate-700 px-1 py-0.5 rounded">admin123</code>).
          </div>
        </div>
      </div>
    </div>
  );
}
