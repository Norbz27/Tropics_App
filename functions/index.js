const admin = require("firebase-admin");
const functions = require("firebase-functions");

admin.initializeApp();

// 🔥 Function to List All Users (Excluding Specific UIDs)
exports.listUsers = functions.https.onRequest(async (req, res) => {
  try {
    let users = [];
    const listUsersResult = await admin.auth().listUsers();

    listUsersResult.users.forEach((userRecord) => {
      if (
        userRecord.uid !== "WmYSRkbNXBWQgFmU9ll33vW0vfm2" &&
        userRecord.uid !== "xWFc9btObrXgCZjlFcENhUtCpfu2" &&
        userRecord.uid !== "GyajFC1CsUTdfe9M18jJzFWkbCX2" &&
        userRecord.uid !== "MPea7izLJCghCIw3z8kfmUXpq683" &&
        userRecord.uid !== "nW1FNVr304YY9red5fKvT9d1lOo2"
      ) {
        users.push({
          uid: userRecord.uid,
          email: userRecord.email,
          name: userRecord.displayName || "No Name", // Include display name
        });
      }
    });

    res.status(200).json(users);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// 🔥 Function to Delete User by Email
exports.deleteUserByEmail = functions.https.onRequest(async (req, res) => {
  const email = req.body.email;

  if (!email) {
    return res.status(400).json({ error: "Email is required" });
  }

  try {
    const user = await admin.auth().getUserByEmail(email);
    await admin.auth().deleteUser(user.uid);
    return res.status(200).json({ message: `User ${email} deleted successfully.` });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});
