import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{js,jsx}'],
    extends: [
      js.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
  },
  {
    files: ['src/shared/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          group: ['**/features/**', '**/app/**', '**/prototypes/**'],
          message: 'shared chỉ chứa mã dùng chung, không phụ thuộc app, features hoặc prototypes.',
        }],
      }],
    },
  },
  {
    files: ['src/app/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          group: ['**/prototypes/**'],
          message: 'Không đưa bản mẫu prototypes vào ứng dụng chính.',
        }],
      }],
    },
  },
  {
    files: ['src/features/**/*.{js,jsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          group: ['**/app/**', '**/prototypes/**'],
          message: 'Feature không phụ thuộc app hoặc prototypes; app chịu trách nhiệm lắp ghép.',
        }],
      }],
    },
  },
  {
    files: ['tests/**/*.js'],
    languageOptions: { globals: globals.node },
  },
])
