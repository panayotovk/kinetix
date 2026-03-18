---
name: deploy
description: Full redeploy of the Kinetix platform — stops all services, rebuilds images from scratch, and starts the full stack. Invoke with /deploy.
user-invocable: true
allowed-tools: Bash
---

# Deploy Kinetix

Run the full redeploy script to rebuild and restart all services.

```bash
cd /home/opc/app/kinetix && ./deploy/redeploy.sh
```

Run this command and stream the output to the user. The script will:

1. Stop all running services
2. Rebuild the Kotlin builder image (no cache)
3. Create the Docker network if needed
4. Start infrastructure and ensure databases exist
5. Rebuild all service images (no cache)
6. Start the full stack

This is a long-running operation. Use a generous timeout (10 minutes).

When complete, report the final status summary from the script output.
