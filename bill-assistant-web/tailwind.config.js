/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],

  theme: {
    extend: {
      /* ✅ Keyframes */
      keyframes: {
        fadeIn: {
          from: { opacity: "0", transform: "translateY(2px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },

        /* ✅ Shimmer Animation */
        shimmer: {
          "100%": {
            transform: "translateX(100%)",
          },
        },
      },

      /* ✅ Animations */
      animation: {
        fadeIn: "fadeIn 0.25s ease-out",

        /* ✅ Premium Skeleton Shimmer */
        shimmer: "shimmer 1.8s infinite",
      },
    },
  },

  plugins: [],
};
