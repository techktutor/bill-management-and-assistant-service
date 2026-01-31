/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
    keyframes: {
      fadeIn: {
        from: { opacity: "0", transform: "translateY(2px)" },
        to: { opacity: "1", transform: "translateY(0)" },
      },
    },
    animation: {
      fadeIn: "fadeIn 0.25s ease-out",
    },
    },
  },
  plugins: [],
};
