import { useState } from "react";
import { uploadBills } from "../api/ingestApi";

export default function UploadBills() {
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [dragActive, setDragActive] = useState(false);

  const startUpload = async (selectedFiles) => {
    if (!selectedFiles || selectedFiles.length === 0) return;

    setUploading(true);
    setResult(null);
    setFiles(selectedFiles);

    try {
      const res = await uploadBills(selectedFiles);
      setResult({
        success: true,
        message: `${res.data.length} bill(s) processed successfully.`,
      });
      setFiles([]);
    } catch {
      setResult({
        success: false,
        message: "Failed to upload bills. Please try again.",
      });
    } finally {
      setUploading(false);
    }
  };

  const handleFileChange = (e) => {
    const selected = Array.from(e.target.files || []);
    startUpload(selected);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const droppedFiles = Array.from(e.dataTransfer.files || []);
    startUpload(droppedFiles);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDragEnter = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
  };

  return (
    <div className="bg-white rounded-xl shadow p-6 max-w-xl">
      <h2 className="text-2xl font-bold mb-4">Upload Bills</h2>

      <p className="text-slate-600 mb-4">
        Drag & drop your bill PDFs or images. Upload starts automatically.
      </p>

      {/* Drag & Drop Zone */}
      <div
        className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition ${
          dragActive
            ? "border-blue-600 bg-blue-50"
            : "border-slate-300 hover:border-blue-500"
        } ${uploading ? "opacity-50 pointer-events-none" : ""}`}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragEnter={handleDragEnter}
        onDragLeave={handleDragLeave}
        onClick={() => document.getElementById("fileInput").click()}
      >
        <input
          id="fileInput"
          type="file"
          multiple
          onChange={handleFileChange}
          disabled={uploading}
          className="hidden"
        />

        <div className="text-4xl mb-2">📂</div>
        <p className="text-sm text-slate-700">
          Drag & drop files here, or <span className="text-blue-600">click to browse</span>
        </p>

        {uploading && (
          <p className="mt-2 text-sm text-blue-600">Uploading…</p>
        )}
      </div>

      {/* Result */}
      {result && (
        <div
          className={`mt-4 p-3 rounded-lg text-sm ${
            result.success
              ? "bg-green-100 text-green-800"
              : "bg-red-100 text-red-800"
          }`}
        >
          {result.message}
        </div>
      )}
    </div>
  );
}
