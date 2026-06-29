import React, { createContext, useContext, useEffect, useState, useRef } from 'react'
import ReconnectingWebSocket from 'reconnecting-websocket'
import { useAuth } from './AuthContext.tsx'

interface SocketContextType {
  socket: ReconnectingWebSocket | null
  isConnected: boolean
  sendMessage: (type: string, payload: any) => void
  subscribe: (type: string, callback: (payload: any) => void) => void
  unsubscribe: (type: string, callback: (payload: any) => void) => void
}

const SocketContext = createContext<SocketContextType | null>(null)

export const SocketProvider = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated, accessToken } = useAuth()
  const [isConnected, setIsConnected] = useState(false)
  const socketRef = useRef<ReconnectingWebSocket | null>(null)
  const listenersRef = useRef<Map<string, Set<(payload: any) => void>>>(new Map())

  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      if (socketRef.current) {
        socketRef.current.close()
        socketRef.current = null
        setIsConnected(false)
      }
      return
    }

    const wsUrl = `ws://localhost:8080/ws/game?token=${accessToken}`
    const rws = new ReconnectingWebSocket(wsUrl, [], {
      connectionTimeout: 4000,
      maxRetries: 10
    })

    socketRef.current = rws

    const handleOpen = () => {
      setIsConnected(true)
      console.log('WebSocket connected')
    }

    const handleClose = () => {
      setIsConnected(false)
      console.log('WebSocket disconnected')
    }

    const handleMessage = (event: MessageEvent) => {
      try {
        const message = JSON.parse(event.data)
        const { type, payload } = message
        const typeListeners = listenersRef.current.get(type)
        if (typeListeners) {
          typeListeners.forEach((callback) => callback(payload))
        }
      } catch (err) {
        console.error('Failed to parse WebSocket message', err)
      }
    }

    rws.addEventListener('open', handleOpen)
    rws.addEventListener('close', handleClose)
    rws.addEventListener('message', handleMessage)

    return () => {
      rws.removeEventListener('open', handleOpen)
      rws.removeEventListener('close', handleClose)
      rws.removeEventListener('message', handleMessage)
      rws.close()
      socketRef.current = null
      setIsConnected(false)
    }
  }, [isAuthenticated, accessToken])

  const sendMessage = (type: string, payload: any) => {
    if (socketRef.current && socketRef.current.readyState === ReconnectingWebSocket.OPEN) {
      socketRef.current.send(JSON.stringify({ type, payload }))
    } else {
      console.warn('Socket not open, cannot send message:', type)
    }
  }

  const subscribe = (type: string, callback: (payload: any) => void) => {
    if (!listenersRef.current.has(type)) {
      listenersRef.current.set(type, new Set())
    }
    listenersRef.current.get(type)!.add(callback)
  }

  const unsubscribe = (type: string, callback: (payload: any) => void) => {
    const typeListeners = listenersRef.current.get(type)
    if (typeListeners) {
      typeListeners.delete(callback)
      if (typeListeners.size === 0) {
        listenersRef.current.delete(type)
      }
    }
  }

  return (
    <SocketContext.Provider
      value={{
        socket: socketRef.current,
        isConnected,
        sendMessage,
        subscribe,
        unsubscribe
      }}
    >
      {children}
    </SocketContext.Provider>
  )
}

export const useSocket = () => {
  const context = useContext(SocketContext)
  if (!context) {
    throw new Error('useSocket must be used within a SocketProvider')
  }
  return context
}
