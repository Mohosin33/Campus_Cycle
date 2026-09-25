async function sendMessage() {
  const input = document.getElementById("userInput").value.trim();
  const responseElement = document.getElementById("response");

  if (!input) {
    responseElement.innerText = "Please type a message first.";
    return;
  }

  responseElement.innerText = "Waiting for reply...";

  try {
    const res = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": "Bearer YOUR_API_KEY",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        messages: [{ role: "user", content: input }]
      })
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      responseElement.innerText = "Error: " + (errorData.error?.message || res.statusText);
      return;
    }

    const data = await res.json();
    responseElement.innerText = data.choices?.[0]?.message?.content || "No reply returned.";
  } catch (error) {
    responseElement.innerText = "Request failed: " + error.message;
  }
}