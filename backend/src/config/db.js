const { Pool } = require("pg");

let pool;

function getPool() {
  if (!process.env.DATABASE_URL) {
    return null;
  }

  if (!pool) {
    pool = new Pool({
      connectionString: process.env.DATABASE_URL
    });
  }

  return pool;
}

async function checkDatabase() {
  const database = getPool();
  if (!database) {
    return {
      ok: false,
      message: "DATABASE_URL is not configured"
    };
  }

  try {
    const result = await database.query("SELECT NOW() AS server_time");
    return {
      ok: true,
      serverTime: result.rows[0].server_time
    };
  } catch (error) {
    return {
      ok: false,
      message: error.message
    };
  }
}

module.exports = {
  getPool,
  checkDatabase
};
