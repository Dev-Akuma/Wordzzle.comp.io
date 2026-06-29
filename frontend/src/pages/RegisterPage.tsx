import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Gamepad2, AlertCircle, CheckCircle } from 'lucide-react'
import { useAuth } from '../context/AuthContext.tsx'

export default function RegisterPage() {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const { register } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSuccess('')

    if (password !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    if (password.length < 6) {
      setError('Password must be at least 6 characters long')
      return
    }

    setIsLoading(true)

    try {
      await register(username, email, password)
      setSuccess('Account created successfully! Redirecting to login...')
      setTimeout(() => {
        navigate('/login')
      }, 2000)
    } catch (err: any) {
      console.error(err)
      setError(err.response?.data?.message || 'Failed to register account')
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-dark-950 flex items-center justify-center relative overflow-hidden px-6">
      {/* Background Neon Glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-neon-purple/5 blur-[120px] rounded-full"></div>

      <div className="w-full max-w-md z-10 py-8">
        {/* Brand Header */}
        <div className="flex flex-col items-center mb-6">
          <Link to="/" className="flex items-center gap-2 mb-2">
            <Gamepad2 className="w-10 h-10 text-neon-purple" />
            <span className="font-extrabold text-3xl tracking-wider bg-gradient-to-r from-neon-purple to-neon-cyan bg-clip-text text-transparent">
              COMP.IO
            </span>
          </Link>
          <p className="text-gray-400 text-sm">Create your puzzle identity</p>
        </div>

        {/* Register Card */}
        <div className="glass p-8 rounded-2xl border border-dark-800 shadow-[0_10px_30px_rgba(0,0,0,0.5)]">
          <h2 className="text-2xl font-black mb-6 text-center">Register</h2>

          {error && (
            <div className="flex items-center gap-2 p-3.5 mb-6 rounded-lg bg-neon-rose/10 border border-neon-rose/30 text-neon-rose text-sm">
              <AlertCircle className="w-5 h-5 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="flex items-center gap-2 p-3.5 mb-6 rounded-lg bg-neon-emerald/10 border border-neon-emerald/30 text-neon-emerald text-sm">
              <CheckCircle className="w-5 h-5 flex-shrink-0" />
              <span>{success}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                Username
              </label>
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Enter username (min 3 chars)"
                className="w-full px-4 py-3 rounded-xl bg-dark-950 border border-dark-700 focus:border-neon-purple focus:ring-1 focus:ring-neon-purple focus:outline-none transition-all text-gray-100 placeholder:text-gray-600"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                Email Address
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Enter email address"
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
                placeholder="Enter password (min 6 chars)"
                className="w-full px-4 py-3 rounded-xl bg-dark-950 border border-dark-700 focus:border-neon-purple focus:ring-1 focus:ring-neon-purple focus:outline-none transition-all text-gray-100 placeholder:text-gray-600"
              />
            </div>

            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">
                Confirm Password
              </label>
              <input
                type="password"
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Re-enter password"
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
                'Create Account'
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-400">
            Already have an account?{' '}
            <Link to="/login" className="text-neon-purple hover:underline font-bold">
              Sign In here
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}
