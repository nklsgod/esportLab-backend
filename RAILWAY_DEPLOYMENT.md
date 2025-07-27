# Railway Deployment Guide

This guide explains how to deploy the esportLab backend to Railway.

## Prerequisites

1. [Railway CLI](https://docs.railway.app/develop/cli) installed
2. Railway account created
3. GitHub repository pushed with latest changes

## Step 1: Login to Railway

```bash
railway login
```

## Step 2: Create a New Railway Project

```bash
railway new
```

Choose:
- **Template**: Empty Project
- **Project Name**: `esportlab-backend`

## Step 3: Add PostgreSQL Database

```bash
railway add postgresql
```

This will automatically create a PostgreSQL database and set the environment variables.

## Step 4: Connect Your GitHub Repository

1. Go to your Railway dashboard
2. Select your project
3. Click "Deploy from GitHub repo"
4. Select `nklsgod/esportLab-backend`
5. Railway will automatically detect the Dockerfile and railway.json

## Step 5: Configure Environment Variables

In the Railway dashboard, go to Variables and add these environment variables:

### Required Variables:
```
SPRING_PROFILES_ACTIVE=prod
DISCORD_CLIENT_ID=your_discord_client_id
DISCORD_CLIENT_SECRET=your_discord_client_secret
DISCORD_REDIRECT_URI=https://your-railway-domain.railway.app/auth/discord/callback
COOKIE_SECURE=true
```

### Optional Variables (for full functionality):
```
CLOUDINARY_CLOUD_NAME=your_cloudinary_name
CLOUDINARY_API_KEY=your_cloudinary_key
CLOUDINARY_API_SECRET=your_cloudinary_secret
DISCORD_BOT_TOKEN=your_discord_bot_token
DISCORD_WEBHOOK_URL=your_discord_webhook_url
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

### Application Configuration:
```
REMINDER_HOURS=0,6,12,18
TIMEZONE=Europe/Berlin
MIN_PLAYERS=4
MIN_DURATION_MINUTES=90
```

**Note**: The database connection variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`) are automatically set by Railway when you add PostgreSQL.

## Step 6: Deploy

Railway will automatically deploy when you push to your GitHub repository. You can also trigger a manual deployment:

```bash
railway up
```

## Step 7: Verify Deployment

1. Check the deployment logs in Railway dashboard
2. Visit your app URL: `https://your-app-name.railway.app/actuator/health`
3. You should see a health check response

## Step 8: Setup Discord OAuth Application

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application or use existing one
3. Go to OAuth2 settings
4. Add redirect URI: `https://your-railway-domain.railway.app/auth/discord/callback`
5. Copy Client ID and Client Secret to Railway environment variables

## Environment Variables Summary

Railway will automatically set these database variables:
- `DATABASE_URL` (Railway format)
- `DB_URL` (Spring Boot format)
- `DB_USER`
- `DB_PASSWORD`

You need to manually set these:
- `SPRING_PROFILES_ACTIVE=prod`
- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `DISCORD_REDIRECT_URI`
- `COOKIE_SECURE=true`

## Troubleshooting

### Common Issues:

1. **Database Connection Failed**
   - Ensure PostgreSQL service is running in Railway
   - Check database environment variables are set correctly

2. **Health Check Failing**
   - Check application logs in Railway dashboard
   - Verify port configuration (Railway sets `PORT` automatically)

3. **Discord OAuth Not Working**
   - Verify Discord redirect URI matches your Railway domain
   - Check Discord Client ID and Secret are correct

### Viewing Logs:
```bash
railway logs
```

### Connecting to Database:
```bash
railway connect postgresql
```

## Local Development with Railway Database

To use Railway's PostgreSQL database locally:

```bash
railway run
```

This will run your local application with Railway's environment variables.

## Custom Domain (Optional)

1. Go to Railway dashboard
2. Select your project
3. Go to Settings > Domains
4. Add your custom domain
5. Update Discord OAuth redirect URI to use your custom domain

## Production Checklist

- [ ] PostgreSQL database added and connected
- [ ] All environment variables configured
- [ ] Discord OAuth application configured with correct redirect URI
- [ ] Health check endpoint working (`/actuator/health`)
- [ ] Application logs show successful startup
- [ ] Database migrations executed successfully
- [ ] HTTPS/SSL working (automatic with Railway)

Your esportLab backend should now be running on Railway! 🚀