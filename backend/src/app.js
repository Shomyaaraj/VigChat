require("dotenv").config();

const express = require("express");
const cors = require("cors");

const healthRoutes = require("./routes/healthRoutes");

const app = express();
const port = process.env.PORT || 5000;

app.use(cors());
app.use(express.json());

app.get("/", (req, res) => {
  res.json({
    service: "vigchat-backend",
    status: "ok",
    message: "Backend running"
  });
});

app.use("/api", healthRoutes);

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});
