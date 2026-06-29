import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Keyboard, AlertCircle } from 'lucide-react'
import { useAuth } from '../context/AuthContext.tsx'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setIsLoading(true)
    
    try {
      await login(username, password)
      navigate('/dashboard')
    } catch (err: any) {
      console.error(err)
      setError(err.response?.data?.message || 'Invalid username or password')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-dark-950 flex items-center justify-center relative overflow-hidden px-6">
      {/* Background Neon Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-neon-purple/5 blur-[120px] rounded-full"></div>

      <div className="w-full max-w-md z-10">
        {/* Brand Header */}
        <div className="flex flex-col items-center mb-8">
          <Link to="/" className="flex items-center gap-2 mb-2">
            <Keyboard className="w-10 h-10 text-neon-purple" />
            <span className="font-extrabold text-3xl tracking-wider bg-gradient-to-r from-neon-purple to-neon-cyan bg-clip-text text-transparent">
              WORDZZLE
            </span>
          </Link>
          <p className="text-gray-400 text-sm">Enter the multiplayer word arena</p>
        </div>

        {/* Login Card */}
        <div className="glass p-8 rounded-2xl border border-dark-800 shadow-[0_10px_30px_rgba(0,0,0,0.5)]">
          <h2 className="text-2xl font-black mb-6 text-center">Login</h2>

          {error && (
            <div className="flex items-center gap-2 p-3.5 mb-6 rounded-lg bg-neon-rose/10 border border-neon-rose/30 text-neon-rose text-sm">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                Username
              </label>
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter username"
                className="w-full px-4 py-3 rounded-xl bg-dark-950 border border-dark-700 focus:border-neon-purple focus:ring-1 focus:ring-neon-purple focus:outline-none transition-all text-gray-100 placeholder:text-gray-600"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                Password
              </label>
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter password"
                className="w-full px-4 py-3 rounded-xl bg-dark-950 border border-dark-700 focus:border-neon-purple focus:ring-1 focus:ring-neon-purple focus:outline-none transition-all text-gray-100 placeholder:text-gray-600"
              />
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full py-3.5 rounded-xl font-bold bg-neon-purple hover:bg-neon-purple/90 shadow-[0_0_15px_rgba(147,51,234,0.3)] transition-all hover:scale-[1.01] active:scale-[0.99] disabled:opacity-50 disabled:pointer-events-none flex items-center justify-center"
            >
              {isLoading ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          <p className="mt-8 text-center text-sm text-gray-400">
            Don't have an account?{' '}
            <Link to="/register" className="text-neon-purple hover:underline font-bold">
              Register here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
