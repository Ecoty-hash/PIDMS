import { resolveConfig } from 'vite';
const c = await resolveConfig({}, 'serve', 'development');
console.log('server.host  =', c.server.host);
console.log('server.port  =', c.server.port);
console.log('server.proxy /api ->', c.server.proxy['/api'].target);
const p = await resolveConfig({}, 'serve', 'production');
console.log('preview.host =', p.preview.host);
console.log('preview.port =', p.preview.port);
console.log('preview.proxy /api ->', p.preview.proxy?.['/api']?.target);
