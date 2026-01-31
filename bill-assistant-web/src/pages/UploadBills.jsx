import { useState } from "react";
import { uploadBills } from "../api/ingestApi";

export default function UploadBills() {
  const [files, setFiles] = useState([]);
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);
  const [dragActive, setDragActive] = useState(false);

  const handleFiles = (newFiles) => {
    setFiles(Array.from(newFiles));
    setResult(null);
  };

  const handleFileChange = (e) => {
    handleFiles(e.target.files);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFiles(e.dataTransfer.files);
      e.dataTransfer.clearData();
    }
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

  const handleUpload = async () => {
    if (files.length === 0) return;

    setUploading(true);
    setResult(null);

    try {
      const res = await uploadBills(files);
      setResult({
        success: true,
        message: `${res.data.length} bill(s) processed successfully.`,
      });
      setFiles([]);
    } catch (err) {
      setResult({
        success: false,
        message: "Failed to upload bills. Please try again.",
      });
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow p-6 max-w-xl">
      <h2 className="text-2xl font-bold mb-4">Upload Bills</h2>

      <p className="text-slate-600 mb-4">
        Upload your bill PDFs or images. You can drag & drop files below or
        click to select.
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
          Drag & drop files here, or{" "}
          <span className="text-blue-600">click to browse</span>
        </p>
      </div>

      {/* Selected files */}
      {files.length > 0 && (
        <ul className="mt-4 text-sm text-slate-700 list-disc list-inside">
          {files.map((f, i) => (
            <li key={i}>{f.name}</li>
          ))}
        </ul>
      )}

      {/* Upload button */}
      <button
        type="button"
        disabled={uploading || files.length === 0}
        onClick={handleUpload}
        className="mt-4 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white px-6 py-2 rounded-lg"
      >
        {uploading ? "Uploading…" : "Upload Bills"}
      </button>

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
