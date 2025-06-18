export async function onboardIssuer(apiUrl: string, issuerData: any) {
  const response = await fetch(`${apiUrl}/onboard/issuer`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(issuerData),
  });
  if (!response.ok) throw new Error("Failed to onboard issuer");
  return response.json();
}