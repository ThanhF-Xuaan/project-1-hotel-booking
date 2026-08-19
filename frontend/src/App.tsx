function App() {
  // Lấy biến môi trường ra xài bằng cú pháp import.meta.env
  const apiUrl = import.meta.env.VITE_API_BASE_URL;
  console.log("Đường dẫn Backend đang gọi là:", apiUrl);

  return (
    <div className="flex items-center justify-center h-screen bg-gray-900">
      <h1 className="text-4xl font-bold text-blue-400">
        🚀 Frontend Hotel Booking Đã Sẵn Sàng!
      </h1>
    </div>
  )
}

export default App