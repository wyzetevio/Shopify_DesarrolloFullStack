import axios from 'axios'
import { ErrorApi, normalizarError } from './errores'
import { sesionPropietario, tiendaActual } from './sesion'

export const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

if (!/^https?:\/\//.test(API_URL)) {
  console.error(`VITE_API_URL debe ser una URL completa (https://...). Valor actual: "${API_URL}"`)
}

/**
 * Si la URL de la API está mal, el hosting del frontend responde con su index.html.
 * Se trata como error para que la página muestre un mensaje en lugar de romperse.
 */
function exigirJson(respuesta) {
  const tipo = String(respuesta.headers?.['content-type'] ?? '')
  if (tipo.includes('text/html')) {
    return Promise.reject(new ErrorApi({
      status: 0,
      mensaje: 'No se pudo conectar con el servidor. Revisa la configuración de la API.',
    }))
  }
  return respuesta
}

export function crearInstancia() {
  const instancia = axios.create({ baseURL: `${API_URL}/api` })
  instancia.interceptors.response.use(exigirJson)
  return instancia
}

/** Rutas públicas, sin sesión: planes, catálogo de una tienda, login y registro. */
export const apiPublica = crearInstancia()
apiPublica.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => Promise.reject(normalizarError(error)),
)

// El AuthContext registra aquí qué hacer cuando el token del propietario deja de valer.
let alExpirarSesionPropietario = () => {}
export function configurarAlExpirarSesionPropietario(funcion) {
  alExpirarSesionPropietario = funcion
}

/** Rutas del propietario: envía su token y, si eligió una, la tienda actual. */
export const apiPropietario = crearInstancia()
apiPropietario.interceptors.request.use((config) => {
  const sesion = sesionPropietario.obtener()
  if (sesion?.token) {
    config.headers.Authorization = `Bearer ${sesion.token}`
  }
  // El backend solo la lee en las rutas de una tienda (productos, categorías, pedidos...)
  // y la ignora en /auth, /tiendas y /usuarios, así que se puede enviar siempre.
  const tiendaId = tiendaActual.obtener()
  if (tiendaId) {
    config.headers['X-Tenant-ID'] = String(tiendaId)
  }
  return config
})
apiPropietario.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    const errorApi = normalizarError(error)
    if (errorApi.status === 401) {
      alExpirarSesionPropietario()
    }
    return Promise.reject(errorApi)
  },
)
