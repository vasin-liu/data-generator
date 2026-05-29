/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ENABLE_MIGRATION?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module '*.css' {}
