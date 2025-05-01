import { Link } from "react-router-dom"
export default function Homepage() {
  return (
    <div className="h-screen flex items-center justify-center bg-gray-50">
    <div className="text-center max-w-md p-8 bg-white rounded-lg shadow-lg">
      <h1 className="text-2xl font-bold mb-4">نظام خرائط الأبراج</h1>
      <p className="mb-6 text-gray-600">
        مرحبًا بك في نظام خرائط الأبراج. اختر إحدى الوظائف التالية للبدء.
      </p>
      <div className="flex flex-col gap-4">
        <Link to="/tower-map" className="p-4 bg-blue-50 hover:bg-blue-100 text-blue-600 rounded-lg transition">
          <div className="font-bold mb-1">خريطة الأبراج</div>
          <div className="text-sm">إيجاد أفضل مسار بين برجين</div>
        </Link>
        <Link to="/pop-map" className="p-4 bg-purple-50 hover:bg-purple-100 text-purple-600 rounded-lg transition">
          <div className="font-bold mb-1">خريطة شبكة POP</div>
          <div className="text-sm">إنشاء شبكة من نقطة مركزية إلى عدة وجهات</div>
        </Link>
      </div>
    </div>
  </div>
  )
}