import React from 'react'
import { Link } from 'react-router-dom'
import { Gamepad2, Trophy, Zap, Swords } from 'lucide-react'
import { useAuth } from '../context/AuthContext.tsx'

export default function LandingPage() {
  const { isAuthenticated } = useAuth()

  return (
    <div className="min-h-screen bg-dark-950 flex flex-col relative overflow-hidden">
      {/* Background Neon Grid effects */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,rgba(147,51,234,0.1),transparent_50%)]"></div>
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-neon-purple/5 blur-[120px] rounded-full"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-neon-cyan/5 blur-[120px] rounded-full"></div>

      {/* Header */}
      <header className="w-full max-w-7xl mx-auto px-6 py-6 flex items-center justify-between z-10">
        <div className="flex items-center gap-2">
          <Gamepad2 className="w-8 h-8 text-neon-purple" />
          <span className="font-extrabold text-2xl tracking-wider bg-gradient-to-r from-neon-purple to-neon-cyan bg-clip-text text-transparent">
            COMP.IO
          </span>
        </div>
        
        <div className="flex items-center gap-4">
          {isAuthenticated ? (
            <Link to="/dashboard" className="px-5 py-2 rounded-lg font-bold text-sm bg-neon-purple hover:bg-neon-purple/80 shadow-[0_0_15px_rgba(147,51,234,0.3)] transition-all">
              Dashboard
            </Link>
          ) : (
            <>
              <Link to="/login" className="px-5 py-2 rounded-lg font-bold text-sm hover:text-neon-purple transition-colors">
                Login
              </Link>
              <Link to="/register" className="px-5 py-2 rounded-lg font-bold text-sm bg-neon-purple hover:bg-neon-purple/80 shadow-[0_0_15px_rgba(147,51,234,0.3)] transition-all">
                Register
              </Link>
            </>
          )}
        </div>
      </header>

      {/* Hero Section */}
      <main className="flex-1 flex flex-col items-center justify-center text-center px-6 max-w-4xl mx-auto z-10 py-12">
        <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full border border-neon-purple/30 bg-neon-purple/10 text-neon-purple text-xs font-bold uppercase tracking-wider mb-6 animate-pulse">
          <Swords className="w-4 h-4" />
          Real-time Matchmaking is Live
        </div>
        
        <h1 className="text-5xl md:text-7xl font-black tracking-tight mb-6">
          Puzzle Games. <br />
          <span className="bg-gradient-to-r from-neon-purple via-neon-cyan to-neon-emerald bg-clip-text text-transparent">
            Multiplayer Arena.
          </span>
        </h1>
        
        <p className="text-gray-400 text-lg md:text-xl max-w-2xl mb-10 leading-relaxed font-light">
          Compete head-to-head in real-time. Same puzzle, same timer. The faster mind wins. Enter Wordzzle, our competitive word battle.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 justify-center w-full max-w-md mb-20">
          <Link
            to={isAuthenticated ? "/dashboard" : "/login"}
            className="flex-1 px-8 py-4 rounded-xl font-extrabold text-lg text-center bg-gradient-to-r from-neon-purple to-neon-cyan hover:brightness-110 shadow-[0_0_25px_rgba(147,51,234,0.4)] transition-all hover:scale-[1.02]"
          >
            Play Ranked
          </Link>
          <Link
            to="/register"
            className="flex-1 px-8 py-4 rounded-xl font-extrabold text-lg text-center border border-dark-700 bg-dark-900/40 hover:bg-dark-900/80 transition-all hover:scale-[1.02]"
          >
            Create Profile
          </Link>
        </div>

        {/* Feature Highlights */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 w-full text-left">
          <div className="glass p-6 rounded-2xl relative group overflow-hidden">
            <div className="absolute top-0 left-0 w-1 h-full bg-neon-purple"></div>
            <Zap className="w-10 h-10 text-neon-purple mb-4" />
            <h3 className="text-xl font-bold mb-2">Real-time PvP</h3>
            <p className="text-gray-400 text-sm leading-relaxed">
              Match with opponents of similar skill instantly. Race on the exact same board concurrently.
            </p>
          </div>

          <div className="glass p-6 rounded-2xl relative group overflow-hidden">
            <div className="absolute top-0 left-0 w-1 h-full bg-neon-cyan"></div>
            <Trophy className="w-10 h-10 text-neon-cyan mb-4" />
            <h3 className="text-xl font-bold mb-2">Elo Rankings</h3>
            <p className="text-gray-400 text-sm leading-relaxed">
              Win matches, earn Elo ratings, and climb the platform leaderboard. Establish your puzzle dominance.
            </p>
          </div>

          <div className="glass p-6 rounded-2xl relative group overflow-hidden">
            <div className="absolute top-0 left-0 w-1 h-full bg-neon-emerald"></div>
            <Swords className="w-10 h-10 text-neon-emerald mb-4" />
            <h3 className="text-xl font-bold mb-2">Wordzzle Game</h3>
            <p className="text-gray-400 text-sm leading-relaxed">
              Our inaugural game: Competitive Wordle. See opponent progress tiles live while hiding your letters.
            </p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full text-center py-8 text-gray-500 border-t border-dark-900 z-10 text-sm">
        &copy; {new Date().getFullYear()} Comp.io. All rights reserved.
      </footer>
    </div>
  )
}
