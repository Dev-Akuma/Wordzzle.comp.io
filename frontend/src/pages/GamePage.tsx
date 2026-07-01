import React, { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Swords, Clock, AlertTriangle, ArrowLeft, RotateCcw } from 'lucide-react'
import { useAuth } from '../context/AuthContext.tsx'
import { useSocket } from '../context/SocketContext.tsx'
import confetti from 'canvas-confetti'

type FeedbackType = 'CORRECT' | 'PRESENT' | 'ABSENT'

export default function GamePage() {
  const { matchId } = useParams<{ matchId: string }>()
  const { user, updateElo } = useAuth()
  const { subscribe, unsubscribe, sendMessage, isConnected } = useSocket()
  const navigate = useNavigate()

  // Game States
  const [currentGuess, setCurrentGuess] = useState('')
  const [playerGuesses, setPlayerGuesses] = useState<string[]>([])
  const [playerFeedback, setPlayerFeedback] = useState<FeedbackType[][]>([])
  const [playerSolved, setPlayerSolved] = useState(false)
  const [playerFinished, setPlayerFinished] = useState(false)

  // Opponent States
  const [opponentAttempts, setOpponentAttempts] = useState(0)
  const [opponentGrid, setOpponentGrid] = useState<FeedbackType[][]>([])
  const [opponentSolved, setOpponentSolved] = useState(false)

  // System States
  const [timer, setTimer] = useState(300) // 5 minutes standard
  const [shakeRowIndex, setShakeRowIndex] = useState<number | null>(null)
  const [guessError, setGuessError] = useState('')
  const [gameEnded, setGameEnded] = useState(false)
  const [endDetails, setEndDetails] = useState<any>(null)
  const [opponentName, setOpponentName] = useState('Opponent')

  const currentGuessRef = useRef(currentGuess)
  const hiddenInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    currentGuessRef.current = currentGuess
  }, [currentGuess])

  // Timer Countdown Effect
  useEffect(() => {
    if (gameEnded) return
    const interval = setInterval(() => {
      setTimer((prev) => {
        if (prev <= 1) {
          clearInterval(interval)
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(interval)
  }, [gameEnded])

  // WebSocket Subscriptions
  useEffect(() => {
    const handleGuessResult = (payload: any) => {
      setPlayerGuesses((prev) => [...prev, payload.guess])
      setPlayerFeedback((prev) => [...prev, payload.feedback])
      setCurrentGuess('')
      
      if (payload.solved) {
        setPlayerSolved(true)
        setPlayerFinished(true)
      } else if (payload.remainingGuesses <= 0) {
        setPlayerFinished(true)
      }
    }

    const handleOpponentProgress = (payload: any) => {
      setOpponentAttempts(payload.attemptsCount)
      setOpponentGrid((prev) => [...prev, payload.gridRow])
      if (payload.solved) {
        setOpponentSolved(payload.solved)
      }
    }

    const handleReconnect = (payload: any) => {
      setOpponentName(payload.opponentName || 'Opponent')
      
      // Reconstitute player guesses
      if (payload.guesses) {
        setPlayerGuesses(payload.guesses)
      }
      if (payload.feedback) {
        setPlayerFeedback(payload.feedback)
      }
      if (payload.solved) {
        setPlayerSolved(true)
        setPlayerFinished(true)
      }
      
      // Opponent reconnect status
      // We rebuild the opponent board based on attempt counts or if history is sent.
      // Since history wasn't strictly configured in reconnect DB payload, we populate
      // empty rows mapping to opponent attempts or custom structures if available.
      if (payload.opponentAttempts) {
        setOpponentAttempts(payload.opponentAttempts)
      }
    }

    const handleGuessError = (payload: any) => {
      setGuessError(payload.message)
      setShakeRowIndex(playerGuesses.length)
      setTimeout(() => {
        setShakeRowIndex(null)
        setGuessError('')
      }, 500)
    }

    const handleGameEnd = (payload: any) => {
      setEndDetails(payload)
      setGameEnded(true)
      
      // Reveal details
      if (payload.newElo) {
        updateElo(payload.newElo)
      }

      // Celebrate if User Won!
      if (payload.winnerUsername === user?.username) {
        const duration = 3000
        const end = Date.now() + duration

        const frame = () => {
          confetti({
            particleCount: 7,
            angle: 60,
            spread: 55,
            origin: { x: 0, y: 0.6 },
            zIndex: 1000
          })
          confetti({
            particleCount: 7,
            angle: 120,
            spread: 55,
            origin: { x: 1, y: 0.6 },
            zIndex: 1000
          })

          if (Date.now() < end) {
            requestAnimationFrame(frame)
          }
        }
        frame()
      }
    }

    subscribe('SERVER_GUESS_RESULT', handleGuessResult)
    subscribe('SERVER_OPPONENT_PROGRESS', handleOpponentProgress)
    subscribe('MSG_RECONNECT', handleReconnect)
    subscribe('GUESS_ERROR', handleGuessError)
    subscribe('MSG_GAME_END', handleGameEnd)

    // Request match details if needed
    // In our design, MATCH_FOUND payload is parsed and saved. Here we default reconnect fetch.

    return () => {
      unsubscribe('SERVER_GUESS_RESULT', handleGuessResult)
      unsubscribe('SERVER_OPPONENT_PROGRESS', handleOpponentProgress)
      unsubscribe('MSG_RECONNECT', handleReconnect)
      unsubscribe('GUESS_ERROR', handleGuessError)
      unsubscribe('MSG_GAME_END', handleGameEnd)
    }
  }, [subscribe, unsubscribe, playerGuesses.length, user?.username, updateElo])

  // Handle Keyboard Inputs
  const handleKeyPress = (key: string) => {
    if (gameEnded || playerFinished) return

    if (key === 'ENTER') {
      if (currentGuessRef.current.length !== 5) {
        setGuessError('Word must be 5 letters')
        setShakeRowIndex(playerGuesses.length)
        setTimeout(() => {
          setShakeRowIndex(null)
          setGuessError('')
        }, 500)
        return
      }
      sendMessage('CLIENT_SUBMIT_GUESS', {
        matchId,
        guess: currentGuessRef.current
      })
    } else if (key === 'BACKSPACE' || key === 'BACK') {
      setCurrentGuess((prev) => prev.slice(0, -1))
    } else if (/^[A-Z]$/.test(key)) {
      if (currentGuessRef.current.length < 5) {
        setCurrentGuess((prev) => prev + key)
      }
    }
  }

  // Bind Physical Keyboard
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) {
        return
      }
      const key = e.key.toUpperCase()
      if (key === 'ENTER') {
        handleKeyPress('ENTER')
      } else if (key === 'BACKSPACE') {
        handleKeyPress('BACKSPACE')
      } else if (/^[A-Z]$/.test(key)) {
        handleKeyPress(key)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [playerGuesses.length, playerFinished, gameEnded])

  // Forfeit handler
  const handleSurrender = () => {
    if (window.confirm('Are you sure you want to forfeit this match? Your opponent will win.')) {
      sendMessage('CLIENT_SURRENDER', { matchId })
    }
  }

  // Letter colors calculation for keyboard hints
  const getLetterStatuses = () => {
    const statuses: { [key: string]: FeedbackType } = {}
    playerGuesses.forEach((guess, rIndex) => {
      const feedback = playerFeedback[rIndex]
      if (!feedback) return
      for (let i = 0; i < guess.length; i++) {
        const char = guess[i]
        const status = feedback[i]
        if (status === 'CORRECT') {
          statuses[char] = 'CORRECT'
        } else if (status === 'PRESENT') {
          if (statuses[char] !== 'CORRECT') {
            statuses[char] = 'PRESENT'
          }
        } else if (status === 'ABSENT') {
          if (statuses[char] !== 'CORRECT' && statuses[char] !== 'PRESENT') {
            statuses[char] = 'ABSENT'
          }
        }
      }
    })
    return statuses
  }

  const keyStatuses = getLetterStatuses()

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`
  }

  // Keyboard Rows
  const keyboardRows = [
    ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'],
    ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'],
    ['ENTER', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', 'BACK']
  ]

  // Render player Wordle grid helper
  const renderRow = (rowIndex: number) => {
    const isCurrentRow = rowIndex === playerGuesses.length
    const isCompletedRow = rowIndex < playerGuesses.length
    const guessWord = isCompletedRow ? playerGuesses[rowIndex] : (isCurrentRow ? currentGuess : '')
    const feedback = isCompletedRow ? playerFeedback[rowIndex] : null

    return (
      <div
        key={rowIndex}
        className={`grid grid-cols-5 gap-2 w-full max-w-[320px] mx-auto ${
          shakeRowIndex === rowIndex ? 'animate-shake' : ''
        }`}
      >
        {Array.from({ length: 5 }).map((_, i) => {
          const letter = guessWord[i] || ''
          const letterFeedback = feedback ? feedback[i] : null

          let bgClass = 'border-dark-700 bg-transparent'
          let textClass = 'text-white'
          let animClass = ''

          if (isCompletedRow && letterFeedback) {
            animClass = 'animate-flip'
            if (letterFeedback === 'CORRECT') {
              bgClass = 'bg-neon-emerald border-neon-emerald'
            } else if (letterFeedback === 'PRESENT') {
              bgClass = 'bg-neon-amber border-neon-amber'
            } else if (letterFeedback === 'ABSENT') {
              bgClass = 'bg-dark-700 border-dark-700 text-gray-400'
            }
          } else if (letter) {
            bgClass = 'border-neon-purple/50 bg-dark-900 animate-pop'
          }

          return (
            <div
              key={i}
              className={`aspect-square flex items-center justify-center text-2xl font-black rounded-lg border-2 uppercase select-none transition-all ${bgClass} ${textClass} ${animClass}`}
              style={{ animationDelay: `${i * 100}ms` }}
            >
              {letter}
            </div>
          )
        })}
      </div>
    )
  }

  // Render opponent mini progress grid helper
  const renderOpponentGrid = () => {
    return (
      <div className="space-y-1.5 w-full max-w-[160px] mx-auto opacity-80">
        {Array.from({ length: 6 }).map((_, rIndex) => {
          const feedback = opponentGrid[rIndex]
          const isFilled = rIndex < opponentAttempts
          
          return (
            <div key={rIndex} className="grid grid-cols-5 gap-1">
              {Array.from({ length: 5 }).map((_, cIndex) => {
                const cellFeedback = feedback ? feedback[cIndex] : null
                let bgClass = 'bg-dark-900 border border-dark-800'
                
                if (isFilled && cellFeedback) {
                  if (cellFeedback === 'CORRECT') bgClass = 'bg-neon-emerald'
                  else if (cellFeedback === 'PRESENT') bgClass = 'bg-neon-amber'
                  else bgClass = 'bg-dark-700'
                }
                
                return (
                  <div
                    key={cIndex}
                    className={`aspect-square rounded ${bgClass} transition-colors`}
                  />
                )
              })}
            </div>
          )
        })}
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-dark-950 flex flex-col relative pb-8">
      {/* Game Header */}
      <header className="border-b border-dark-800 bg-dark-950/80 backdrop-blur-md sticky top-0 z-20 px-6 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <button
            onClick={() => {
              if (!gameEnded && !playerFinished) {
                if (!window.confirm('Leave match? This will count as a forfeit!')) return
                sendMessage('CLIENT_SURRENDER', { matchId })
              }
              navigate('/dashboard')
            }}
            className="flex items-center gap-1.5 text-sm font-semibold text-gray-400 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Exit Area
          </button>

          <div className="flex items-center gap-4.5 bg-dark-900/80 px-4 py-2 border border-dark-800 rounded-2xl">
            <Clock className={`w-4 h-4 ${timer < 60 ? 'text-neon-rose animate-pulse' : 'text-neon-cyan'}`} />
            <span className={`font-mono font-bold text-lg ${timer < 60 ? 'text-neon-rose' : 'text-gray-200'}`}>
              {formatTimer(timer)}
            </span>
          </div>

          <div className="flex items-center gap-2">
            {!isConnected && (
              <span className="text-xs px-2.5 py-1 rounded bg-neon-rose/10 text-neon-rose border border-neon-rose/20 animate-pulse font-bold">
                Offline
              </span>
            )}
            <span className="text-xs px-2.5 py-1 rounded bg-neon-purple/10 text-neon-purple border border-neon-purple/20 font-bold uppercase tracking-wider">
              Ranked PvP
            </span>
          </div>
        </div>
      </header>

      {/* Main Board Layout */}
      <div className="flex-1 max-w-7xl mx-auto w-full px-6 py-8 flex flex-col md:grid md:grid-cols-4 gap-8">
        
        {/* Left column: Opponent progress feed */}
        <div className="order-2 md:order-1 md:col-span-1 flex flex-col items-center justify-center">
          <div className="glass p-6 rounded-2xl border border-dark-850 w-full text-center">
            <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-2">Live Opponent Feed</h3>
            <p className="font-extrabold text-neon-cyan truncate mb-1">{opponentName}</p>
            <p className="text-xs text-gray-500 mb-6">
              {opponentSolved ? 'Solved! 🎉' : `Attempt ${opponentAttempts}/6`}
            </p>

            {renderOpponentGrid()}
          </div>

          {!gameEnded && !playerFinished && (
            <button
              onClick={handleSurrender}
              className="mt-6 w-full py-3 border border-neon-rose/30 hover:bg-neon-rose/10 hover:border-neon-rose/50 text-neon-rose font-bold text-sm rounded-xl transition-all"
            >
              Surrender
            </button>
          )}
        </div>

        {/* Center / Right Columns: Solve Arena */}
        <div className="order-1 md:order-2 md:col-span-3 flex flex-col justify-between items-center space-y-8">
          
          {/* Main Wordle Grid */}
          <div 
            className="space-y-2 w-full flex flex-col items-center justify-center cursor-pointer"
            onClick={() => hiddenInputRef.current?.focus()}
          >
            {/* Hidden input to summon native mobile keyboard */}
            <input
              ref={hiddenInputRef}
              type="text"
              className="opacity-0 absolute -z-10"
              autoComplete="off"
              autoCorrect="off"
              autoCapitalize="characters"
              spellCheck="false"
              value={currentGuess}
              onChange={(e) => {
                const val = e.target.value.toUpperCase().replace(/[^A-Z]/g, '').slice(0, 5)
                setCurrentGuess(val)
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleKeyPress('ENTER')
                }
              }}
            />
            {Array.from({ length: 6 }).map((_, rIndex) => renderRow(rIndex))}
            
            {guessError && (
              <p className="text-sm text-neon-rose font-semibold mt-4 text-center">{guessError}</p>
            )}
          </div>

          {/* Keypad */}
          <div className="w-full max-w-[500px]">
            <div className="flex flex-col gap-1.5">
              {keyboardRows.map((row, rIdx) => (
                <div key={rIdx} className="flex justify-center gap-1">
                  {row.map((key) => {
                    const status = keyStatuses[key]
                    let keyColor = 'bg-dark-700 hover:bg-dark-600 text-white'
                    
                    if (status === 'CORRECT') {
                      keyColor = 'bg-neon-emerald text-white'
                    } else if (status === 'PRESENT') {
                      keyColor = 'bg-neon-amber text-white'
                    } else if (status === 'ABSENT') {
                      keyColor = 'bg-dark-900 border border-dark-800 text-gray-600'
                    }

                    const isSpecialKey = key === 'ENTER' || key === 'BACK'
                    const keyWidth = isSpecialKey ? 'px-4' : 'w-10'

                    return (
                      <button
                        key={key}
                        onClick={() => handleKeyPress(key)}
                        className={`h-12 ${keyWidth} rounded-lg flex items-center justify-center font-bold text-xs uppercase transition-all select-none ${keyColor}`}
                      >
                        {key}
                      </button>
                    )
                  })}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Game End details Modal overlay */}
      {gameEnded && endDetails && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/85 backdrop-blur-md p-6">
          <div className="glass max-w-md w-full p-8 rounded-3xl border border-neon-purple/30 text-center animate-pop">
            
            {endDetails.winnerUsername === user?.username ? (
              <div className="mb-4">
                <span className="text-4xl">🏆</span>
                <h2 className="text-3xl font-black text-neon-emerald mt-2">VICTORY!</h2>
              </div>
            ) : endDetails.winnerUsername === 'DRAW' ? (
              <div className="mb-4">
                <span className="text-4xl">🤝</span>
                <h2 className="text-3xl font-black text-gray-300 mt-2">DRAW GAME</h2>
              </div>
            ) : (
              <div className="mb-4">
                <span className="text-4xl">💀</span>
                <h2 className="text-3xl font-black text-neon-rose mt-2">DEFEAT</h2>
              </div>
            )}

            <p className="text-gray-400 text-sm mb-6">
              Reason: <span className="font-bold text-gray-200">{endDetails.endReason}</span>
            </p>

            <div className="bg-dark-900/60 p-4 rounded-2xl border border-dark-800 mb-6 flex flex-col items-center">
              <span className="text-xs text-gray-400 uppercase tracking-widest font-bold mb-1">Target Word</span>
              <span className="text-3xl font-black tracking-widest text-white uppercase text-glow-purple">
                {endDetails.targetWord}
              </span>
            </div>

            {/* Score Comparison table */}
            <div className="grid grid-cols-2 gap-4 mb-8">
              <div className="p-3 bg-dark-950/40 rounded-xl border border-dark-850">
                <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">You</p>
                <p className="text-base font-extrabold">
                  {endDetails.playerScore.solved ? `${endDetails.playerScore.attempts} guesses` : 'Failed'}
                </p>
                <p className="text-xs text-gray-500">
                  {endDetails.playerScore.solved ? `${endDetails.playerScore.timeSeconds}s` : ''}
                </p>
              </div>

              <div className="p-3 bg-dark-950/40 rounded-xl border border-dark-850">
                <p className="text-xs text-gray-400 font-bold uppercase tracking-wider mb-1">{opponentName}</p>
                <p className="text-base font-extrabold">
                  {endDetails.opponentScore.solved ? `${endDetails.opponentScore.attempts} guesses` : 'Failed'}
                </p>
                <p className="text-xs text-gray-500">
                  {endDetails.opponentScore.solved ? `${endDetails.opponentScore.timeSeconds}s` : ''}
                </p>
              </div>
            </div>

            {/* Elo Change block */}
            <div className="border-t border-dark-850 pt-6 mb-8 flex items-center justify-between">
              <div className="text-left">
                <p className="text-xs text-gray-400 font-bold uppercase">New rating</p>
                <p className="text-2xl font-black text-white">Elo {endDetails.newElo}</p>
              </div>
              <div className={`px-4 py-2 rounded-xl text-lg font-extrabold ${
                endDetails.eloChange >= 0
                  ? 'bg-neon-emerald/10 text-neon-emerald border border-neon-emerald/20'
                  : 'bg-neon-rose/10 text-neon-rose border border-neon-rose/20'
              }`}>
                {endDetails.eloChange >= 0 ? `+${endDetails.eloChange}` : endDetails.eloChange} Elo
              </div>
            </div>

            <button
              onClick={() => navigate('/dashboard')}
              className="w-full py-4 bg-neon-purple hover:bg-neon-purple/90 font-extrabold rounded-xl transition-all shadow-[0_0_15px_rgba(147,51,234,0.3)] flex items-center justify-center gap-2"
            >
              <RotateCcw className="w-5 h-5" />
              Return to Arena Dashboard
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
