/**
 * Environment-specific frontend settings. The product name is configuration, never a literal in components
 * (CLAUDE.md "Naming and branding"); it reaches translations as the `{appName}` parameter.
 */
export const appConfig = {
  appName: import.meta.env.VITE_APP_NAME,
} as const
