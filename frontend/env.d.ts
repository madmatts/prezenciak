/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Product name shown to users ("Wishka"), from frontend/.env */
  readonly VITE_APP_NAME: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
