import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'info.mitalinfosys.workmanager',
  appName: 'MITAL WorkManager',
  webDir: 'dist/public',
  server: {
    androidScheme: 'https',
    cleartext: true,
    allowNavigation: ['web.mitalinfosys.info', '*.mitalinfosys.info']
  },
  android: {
    allowMixedContent: true
  }
};

export default config;
