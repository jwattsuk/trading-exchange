/** @type {import('next').NextConfig} */
const nextConfig = {
  // Enable WebSocket support
  webpack: (config) => {
    config.externals.push({
      'utf-8-validate': 'commonjs utf-8-validate',
      'bufferutil': 'commonjs bufferutil',
    });
    return config;
  },
  // Environment variables
  env: {
    CUSTOM_KEY: 'custom-value',
  },
}

module.exports = nextConfig
