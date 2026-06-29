import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Keyboard, Trophy, Swords, Zap, LogOut, Clock, Activity, EyeOff } from 'lucide-react'
import { useAuth } from '../context/AuthContext.tsx'
import { useSocket } from '../context/SocketContext.tsx'
import { api } from '../services/api.ts'

export default function DashboardPage() {
  const { user, logout } = useAuth()
  const { subscribe, unsubscribe, isConnected } = useSocket()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [queueStatus, setQueueStatus] = useState<'IDLE' | 'QUEUED' | 'MATCHED'>('IDLE')
  const [matchDetails, setMatchDetails] = useState<any>(null)
  const [queueTimer, setQueueTimer] = useState(0)
  const [countdown, setCountdown] = useState(3)

  // Fetch Profile data
  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: () => api.get('/profile').then((res) => res.data)
  })

  // Fetch Leaderboard
  const { data: leaderboard, isLoading: isLeaderboardLoading } = useQuery({
    queryKey: ['leaderboard'],
    queryFn: () => api.get('/leaderboard').then((res) => res.data)
  })

  // Fetch Match History
  const { data: history, isLoading: isHistoryLoading } = useQuery({
    queryKey: ['history'],
    queryFn: () => api.get('/matches/history').then((res) => res.data)
  })

  // Check initial queue status on mount
  useEffect(() => {
    api.get('/queue/status')
      .then((res) => {
        if (res.data.queued) {
          setQueueStatus('QUEUED')
        }
      })
      .catch((err) => console.error('Failed to get queue status', err))
  }, [])

  // Queue Timer effect
  useEffect(() => {
    let interval: any
    if (queueStatus === 'QUEUED') {
      interval = setInterval(() => {
        setQueueTimer((prev) => prev + 1)
      }, 1000)
    } else {
      setQueueTimer(0)
    }
    return () => clearInterval(interval)
  }, [queueStatus])

  // Subscribe to Match Found / Game Start WebSocket events
  useEffect(() => {
    const handleMatchFound = (payload: any) => {
      setQueueStatus('MATCHED')
      setMatchDetails(payload)
      setCountdown(payload.countdownSeconds || 3)
    }

    const handleGameStart = (payload: any) => {
      navigate(`/game/${payload.matchId}`)
    }

    subscribe('MATCH_FOUND', handleMatchFound)
    subscribe('GAME_START', handleGameStart)

    return () => {
      unsubscribe('MATCH_FOUND', handleMatchFound)
      unsubscribe('GAME_START', handleGameStart)
    }
  }, [subscribe, unsubscribe, navigate])

  // Countdown timer when MATCHED
  useEffect(() => {
    let interval: any
    if (queueStatus === 'MATCHED' && countdown > 0) {
      interval = setInterval(() => {
        setCountdown((prev) => prev - 1)
      }, 1000)
    }
    return () => clearInterval(interval)
  }, [queueStatus, countdown])

  // Join Queue Mutation
  const joinQueueMutation = useMutation({
    mutationFn: () => api.post('/queue/join'),
    onSuccess: () => setQueueStatus('QUEUED'),
    onError: (err) => console.error('Error joining queue', err)
  })

  // Leave Queue Mutation
  const leaveQueueMutation = useMutation({
    mutationFn: () => api.post('/queue/leave'),
    onSuccess: () => setQueueStatus('IDLE'),
    onError: (err) => console.error('Error leaving queue', err)
  })

  const handlePlayRanked = () => {
    if (queueStatus === 'IDLE') {
      joinQueueMutation.mutate()
    } else if (queueStatus === 'QUEUED') {
      leaveQueueMutation.mutate()
    }
  }

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`
  }

  return (
    <div className="min-h-screen bg-dark-950 flex flex-col relative">
      {/* Background neon elements */}
      <div className="absolute top-0 right-0 w-[300px] h-[300px] bg-neon-purple/5 blur-[100px] rounded-full pointer-events-none"></div>
      <div className="absolute bottom-0 left-0 w-[300px] h-[300px] bg-neon-cyan/5 blur-[100px] rounded-full pointer-events-none"></div>

      {/* Navigation */}
      <nav className="border-b border-dark-800 bg-dark-950/80 backdrop-blur-md sticky top-0 z-20">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Keyboard className="w-6 h-6 text-neon-purple" />
            <span className="font-extrabold text-xl tracking-wider bg-gradient-to-r from-neon-purple to-neon-cyan bg-clip-text text-transparent">
              WORDZZLE
            </span>
          </div>

          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-neon-emerald animate-pulse"></span>
              <span className="text-xs font-semibold text-gray-400">Lobby Online</span>
            </div>
            
            <div className="h-4 w-px bg-dark-800"></div>

            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-bold text-gray-200">{user?.username}</p>
                <p className="text-xs text-neon-purple font-semibold">Elo {user?.eloRating}</p>
              </div>
              <button
                onClick={logout}
                className="p-2.5 rounded-lg bg-dark-900 border border-dark-850 hover:bg-neon-rose/10 hover:border-neon-rose/30 hover:text-neon-rose transition-all"
                title="Logout"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content Dashboard */}
      <div className="flex-1 max-w-7xl mx-auto w-full px-6 py-8 grid grid-cols-1 lg:grid-cols-3 gap-8 z-10">
        
        {/* Left / Center Panels (Play & Stats) */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Match Finder Panel */}
          <div className="glass p-8 rounded-3xl border border-dark-850 relative overflow-hidden flex flex-col items-center justify-center min-h-[300px]">
            {queueStatus === 'IDLE' && (
              <div className="text-center py-6">
                <Swords className="w-16 h-16 text-neon-purple mx-auto mb-6" />
                <h2 className="text-3xl font-black mb-3">Ranked Matchmaking</h2>
                <p className="text-gray-400 max-w-sm mb-8 text-sm">
                  Compete against players globally. Guess the hidden 5-letter word in fewer attempts and faster times.
                </p>
                <button
                  onClick={handlePlayRanked}
                  disabled={!isConnected}
                  className="px-10 py-4 bg-gradient-to-r from-neon-purple to-neon-cyan hover:brightness-110 shadow-[0_0_20px_rgba(147,51,234,0.3)] transition-all font-extrabold text-lg rounded-xl hover:scale-[1.02] disabled:opacity-50"
                >
                  {isConnected ? 'Play Ranked' : 'Connecting to Server...'}
                </button>
              </div>
            )}

            {queueStatus === 'QUEUED' && (
              <div className="text-center py-6 flex flex-col items-center">
                {/* Radar Searching Animation */}
                <div className="relative w-28 h-28 mb-8 flex items-center justify-center">
                  <div className="absolute inset-0 border border-neon-purple/20 rounded-full"></div>
                  <div className="absolute inset-4 border border-neon-cyan/20 rounded-full"></div>
                  <div className="absolute inset-8 border border-neon-purple/30 rounded-full"></div>
                  <div className="absolute inset-0 border-t-2 border-neon-purple rounded-full animate-radar"></div>
                  <Activity className="w-8 h-8 text-neon-purple animate-pulse" />
                </div>
                
                <h3 className="text-2xl font-black text-neon-purple tracking-wide mb-2">Searching...</h3>
                <p className="text-gray-400 text-sm mb-1">Time in queue: {formatTime(queueTimer)}</p>
                <p className="text-xs text-gray-500 mb-8">Searching in rating range: &plusmn;{(100 + queueTimer * 10)} Elo</p>
                
                <button
                  onClick={handlePlayRanked}
                  className="px-8 py-3 bg-dark-900 border border-dark-700 hover:bg-neon-rose/10 hover:border-neon-rose/30 hover:text-neon-rose transition-all font-bold text-sm rounded-xl"
                >
                  Cancel Search
                </button>
              </div>
            )}

            {queueStatus === 'MATCHED' && matchDetails && (
              <div className="text-center py-6 flex flex-col items-center">
                <div className="flex items-center gap-8 mb-8">
                  <div className="text-center">
                    <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">You</p>
                    <p className="text-lg font-black">{user?.username}</p>
                    <p className="text-xs text-neon-purple">Elo {user?.eloRating}</p>
                  </div>
                  <div className="text-neon-rose font-black text-2xl animate-bounce">VS</div>
                  <div className="text-center">
                    <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">Opponent</p>
                    <p className="text-lg font-black text-neon-cyan">{matchDetails.opponentName}</p>
                    <p className="text-xs text-neon-cyan">Elo {matchDetails.opponentElo}</p>
                  </div>
                </div>
                
                <h4 className="text-xl font-bold mb-4">MATCH FOUND!</h4>
                <div className="text-5xl font-black text-neon-emerald tracking-wide mb-2 animate-pulse">{countdown}</div>
                <p className="text-xs text-gray-500">Preparing board, sync timers...</p>
              </div>
            )}
          </div>

          {/* User Profile Stats Card */}
          <div className="glass p-8 rounded-3xl border border-dark-850">
            <h3 className="text-xl font-black mb-6 flex items-center gap-2">
              <Trophy className="w-5 h-5 text-neon-purple" />
              Your Performance Stats
            </h3>
            
            {isProfileLoading ? (
              <div className="h-32 flex items-center justify-center">
                <div className="w-8 h-8 border-3 border-neon-purple border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : profile ? (
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-6">
                <div className="bg-dark-900/60 p-4.5 rounded-xl border border-dark-800">
                  <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">Games Played</p>
                  <p className="text-2xl font-black">{profile.stats.gamesPlayed}</p>
                </div>
                <div className="bg-dark-900/60 p-4.5 rounded-xl border border-dark-800">
                  <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">Wins / Losses</p>
                  <p className="text-2xl font-black text-neon-emerald">
                    {profile.stats.wins} <span className="text-gray-600">/</span> <span className="text-neon-rose">{profile.stats.losses}</span>
                  </p>
                </div>
                <div className="bg-dark-900/60 p-4.5 rounded-xl border border-dark-800">
                  <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">Win Rate</p>
                  <p className="text-2xl font-black text-neon-cyan">
                    {profile.stats.gamesPlayed > 0 ? Math.round((profile.stats.wins / profile.stats.gamesPlayed) * 100) : 0}%
                  </p>
                </div>
                <div className="bg-dark-900/60 p-4.5 rounded-xl border border-dark-800">
                  <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">Win Streak</p>
                  <p className="text-2xl font-black">{profile.stats.winStreak} 🔥</p>
                </div>
                <div className="bg-dark-900/60 p-4.5 rounded-xl border border-dark-800">
                  <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">Avg Guesses</p>
                  <p className="text-2xl font-black">{profile.stats.avgGuesses.toFixed(1)}</p>
                </div>
                <div className="bg-dark-900/60 p-4.5 rounded-xl border border-dark-800">
                  <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">Avg Solve Time</p>
                  <p className="text-2xl font-black flex items-center gap-1">
                    <Clock className="w-4 h-4 text-neon-cyan" />
                    {profile.stats.avgSolveTimeSeconds.toFixed(0)}s
                  </p>
                </div>
              </div>
            ) : (
              <p className="text-gray-500 text-center py-6">Could not load statistics</p>
            )}
          </div>

          {/* Recent Match Logs */}
          <div className="glass p-8 rounded-3xl border border-dark-850">
            <h3 className="text-xl font-black mb-6">Recent Match History</h3>
            
            {isHistoryLoading ? (
              <div className="h-32 flex items-center justify-center">
                <div className="w-8 h-8 border-3 border-neon-purple border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : history && history.length > 0 ? (
              <div className="space-y-4 max-h-[400px] overflow-y-auto pr-2">
                {history.map((match: any) => (
                  <div key={match.matchId} className="flex items-center justify-between p-4 bg-dark-900/40 border border-dark-800/80 rounded-xl hover:border-dark-700 transition-colors">
                    <div>
                      <p className="text-sm font-bold">vs {match.opponentName}</p>
                      <p className="text-xs text-gray-500">
                        {new Date(match.playedAt).toLocaleDateString()} &middot; {match.attempts} attempts &middot; {match.solveTimeSeconds ? `${match.solveTimeSeconds}s` : 'Failed'}
                      </p>
                    </div>
                    <div className="flex items-center gap-4">
                      <span className={`px-2.5 py-1 rounded text-xs font-extrabold uppercase ${
                        match.result === 'WIN' ? 'bg-neon-emerald/10 text-neon-emerald border border-neon-emerald/30' :
                        match.result === 'LOSS' ? 'bg-neon-rose/10 text-neon-rose border border-neon-rose/30' :
                        'bg-gray-800 text-gray-400 border border-gray-700'
                      }`}>
                        {match.result}
                      </span>
                      <span className={`text-sm font-bold ${match.ratingChange >= 0 ? 'text-neon-emerald' : 'text-neon-rose'}`}>
                        {match.ratingChange >= 0 ? `+${match.ratingChange}` : match.ratingChange}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8 text-gray-500 text-sm">
                No matches played yet. Join the queue to start playing!
              </div>
            )}
          </div>
        </div>

        {/* Right Panel (Leaderboard) */}
        <div>
          <div className="glass p-6 rounded-3xl border border-dark-850 sticky top-[90px]">
            <h3 className="text-xl font-black mb-6 flex items-center gap-2">
              <Trophy className="w-5 h-5 text-neon-amber animate-bounce" />
              Top Players Leaderboard
            </h3>
            
            {isLeaderboardLoading ? (
              <div className="h-40 flex items-center justify-center">
                <div className="w-8 h-8 border-3 border-neon-purple border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : leaderboard && leaderboard.length > 0 ? (
              <div className="space-y-3.5 max-h-[640px] overflow-y-auto pr-2">
                {leaderboard.map((entry: any) => (
                  <div
                    key={entry.username}
                    className={`flex items-center justify-between p-3.5 rounded-xl border transition-all ${
                      entry.username === user?.username
                        ? 'bg-neon-purple/10 border-neon-purple/40 font-bold'
                        : 'bg-dark-900/30 border-dark-800/80 hover:border-dark-750'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-black ${
                        entry.rank === 1 ? 'bg-neon-amber text-dark-950 shadow-[0_0_10px_rgba(245,158,11,0.4)]' :
                        entry.rank === 2 ? 'bg-gray-300 text-dark-950' :
                        entry.rank === 3 ? 'bg-amber-600 text-white' :
                        'bg-dark-800 text-gray-400'
                      }`}>
                        {entry.rank}
                      </span>
                      <span className="text-sm truncate max-w-[120px]">{entry.username}</span>
                    </div>
                    <span className="text-xs font-bold text-neon-purple">Elo {entry.eloRating}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-gray-500 text-center py-6 text-sm">No rankings available</p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
