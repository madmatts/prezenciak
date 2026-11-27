import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginPlaywright from 'eslint-plugin-playwright'
import pluginVitest from '@vitest/eslint-plugin'
import pluginOxlint from 'eslint-plugin-oxlint'
import pluginVueI18n from '@intlify/eslint-plugin-vue-i18n'
import skipFormatting from 'eslint-config-prettier/flat'
import type { Linter } from 'eslint'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**', '**/playwright-report/**']),

  ...pluginVue.configs['flat/recommended'],
  vueTsConfigs.recommended,

  // Golden rule 4: no hard-coded user-facing text; every key used must exist in the dictionaries.
  // The plugin's own types are looser than ESLint's; the config itself is valid.
  ...(pluginVueI18n.configs.recommended as unknown as Linter.Config[]),
  {
    name: 'app/i18n-settings',
    settings: {
      'vue-i18n': {
        localeDir: './src/locales/*.json',
        messageSyntaxVersion: '^11.0.0',
      },
    },
  },
  {
    name: 'app/i18n',
    files: ['src/**/*.vue'],
    rules: {
      '@intlify/vue-i18n/no-raw-text': [
        'error',
        { ignorePattern: '^[-#:()&+×/°′″%.,!?…\\s]+$', ignoreText: [] },
      ],
      '@intlify/vue-i18n/no-missing-keys': 'error',
      '@intlify/vue-i18n/no-dynamic-keys': 'off',
      '@intlify/vue-i18n/no-v-html': 'error',
    },
  },
  {
    ...pluginPlaywright.configs['flat/recommended'],
    files: ['e2e/**/*.{test,spec}.{js,ts,jsx,tsx}'],
  },

  {
    ...pluginVitest.configs.recommended,
    files: ['src/**/__tests__/*'],
  },

  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),

  skipFormatting,
)
