const { checkDatabase } = require("../config/db");

async function getHealth(req, res) {
  const database = await checkDatabase();
  const statusCode = database.ok ? 200 : 503;

  res.status(statusCode).json({
    service: "vigchat-backend",
    status: database.ok ? "ok" : "degraded",
    database,
    timestamp: new Date().toISOString()
  });
}

module.exports = {
  getHealth
};
