import ModeButtons from './ModeButtons'

// Shown when there's no ?room= in the URL
export default function HomeScreen() {
  return (
    <div style={{ padding: "40px" }}>
      <h1>KeyTrace</h1>
      <p style={{ fontSize: "18px" }}>Practice coding interviews, then replay how you solved them.</p>
      <p style={{ fontSize: "16px", opacity: 0.8 }}>
        Solo: just you · Interview: 2 people · Group: up to 10
      </p>
      <div style={{ display: "flex", gap: "16px", marginTop: "20px" }}>
        <ModeButtons />
      </div>
    </div>
  )
}