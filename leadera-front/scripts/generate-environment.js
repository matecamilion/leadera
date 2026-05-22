// Pre-build hook: inyecta las credenciales de Supabase desde env vars
// (definidas en Vercel) hacia environment.prod.ts antes de `ng build`.
// En local, si las env vars no están seteadas, deja el archivo como está
// para no pisar valores que el desarrollador haya puesto manualmente.

const fs = require('fs');
const path = require('path');

const supabaseUrl = process.env.SUPABASE_URL;
const supabaseAnonKey = process.env.SUPABASE_ANON_KEY;
const apiUrl = process.env.API_URL || 'https://leadera-42po.onrender.com';

if (!supabaseUrl || !supabaseAnonKey) {
  console.log(
    '[generate-environment] SUPABASE_URL / SUPABASE_ANON_KEY no presentes — environment.prod.ts no se modifica.'
  );
  process.exit(0);
}

const target = path.join(__dirname, '..', 'src', 'environments', 'environment.prod.ts');
const contenido = `export const environment = {
  production: true,
  apiUrl: '${apiUrl}',
  supabaseUrl: '${supabaseUrl}',
  supabaseAnonKey: '${supabaseAnonKey}',
};
`;

fs.writeFileSync(target, contenido, 'utf8');
console.log('[generate-environment] environment.prod.ts inyectado desde env vars.');
