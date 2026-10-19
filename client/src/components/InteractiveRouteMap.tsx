import { useState } from "react";
import { Plane, Compass, MapPin, Sparkles, Navigation, Globe2, ShieldCheck, Clock } from "lucide-react";

interface CityHub {
  id: string;
  name: string;
  country: string;
  x: number; // SVG coordinate 0-1000
  y: number; // SVG coordinate 0-500
  tripsCount: number;
  timezone: string;
  avgDuration: string;
}

const HUBS: CityHub[] = [
  { id: "blr", name: "Bengaluru", country: "India", x: 670, y: 285, tripsCount: 14, timezone: "GMT+5:30", avgDuration: "HQ Hub" },
  { id: "del", name: "New Delhi", country: "India", x: 660, y: 245, tripsCount: 8, timezone: "GMT+5:30", avgDuration: "2h 45m" },
  { id: "bom", name: "Mumbai", country: "India", x: 648, y: 275, tripsCount: 6, timezone: "GMT+5:30", avgDuration: "1h 45m" },
  { id: "sin", name: "Singapore", country: "Singapore", x: 735, y: 310, tripsCount: 9, timezone: "GMT+8", avgDuration: "4h 15m" },
  { id: "dxb", name: "Dubai", country: "UAE", x: 585, y: 245, tripsCount: 5, timezone: "GMT+4", avgDuration: "3h 50m" },
  { id: "lhr", name: "London", country: "UK", x: 460, y: 175, tripsCount: 4, timezone: "GMT+1", avgDuration: "9h 30m" },
  { id: "fra", name: "Frankfurt", country: "Germany", x: 485, y: 180, tripsCount: 3, timezone: "GMT+2", avgDuration: "8h 45m" },
  { id: "sfo", name: "San Francisco", country: "USA", x: 190, y: 205, tripsCount: 3, timezone: "GMT-7", avgDuration: "19h (1-stop)" },
  { id: "jfk", name: "New York", country: "USA", x: 285, y: 195, tripsCount: 4, timezone: "GMT-4", avgDuration: "15h 20m" },
  { id: "tyo", name: "Tokyo", country: "Japan", x: 835, y: 220, tripsCount: 2, timezone: "GMT+9", avgDuration: "7h 30m" },
];

const FLIGHT_ROUTES = [
  { from: "blr", to: "del" },
  { from: "blr", to: "bom" },
  { from: "blr", to: "sin" },
  { from: "blr", to: "dxb" },
  { from: "blr", to: "lhr" },
  { from: "del", to: "sfo" },
  { from: "bom", to: "lhr" },
  { from: "lhr", to: "jfk" },
  { from: "sin", to: "tyo" },
  { from: "dxb", to: "fra" },
];

export function InteractiveRouteMap() {
  const [selectedHub, setSelectedHub] = useState<CityHub>(HUBS[0]);
  const [hoveredHub, setHoveredHub] = useState<CityHub | null>(null);

  const getHub = (id: string) => HUBS.find((h) => h.id === id);

  return (
    <div className="panel p-6 space-y-5 rounded-2xl bg-white dark:bg-[#111827] border border-[#E4E7EC] dark:border-[#1E293B] shadow-sm">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div>
          <span className="card-kicker">GLOBAL CORPORATE TRAVEL NETWORK</span>
          <h2 className="section-heading flex items-center gap-2">
            <Globe2 size={20} className="text-[#14B8A6]" /> Interactive Flight Corridors
          </h2>
          <p className="text-xs text-[#667085] dark:text-[#94A3B8] mt-0.5">
            Active corporate air corridors and frequency across global offices. Click any hub for flight details.
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs">
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#14B8A6]/10 text-[#0D9488] dark:text-[#2DD4BF] font-semibold border border-[#14B8A6]/20">
            <span className="w-2 h-2 rounded-full bg-[#14B8A6] animate-ping" />
            10 Active Hubs
          </span>
        </div>
      </div>

      {/* SVG Map Container */}
      <div className="relative w-full rounded-2xl bg-gradient-to-b from-[#101828] to-[#0F172A] p-4 overflow-hidden border border-white/10 shadow-inner">
        {/* Subtle grid pattern background */}
        <div
          className="absolute inset-0 opacity-10 pointer-events-none"
          style={{
            backgroundImage: "radial-gradient(#ffffff 1px, transparent 1px)",
            backgroundSize: "24px 24px",
          }}
        />

        <svg
          viewBox="0 0 1000 480"
          className="w-full h-auto max-h-[380px] drop-shadow-md"
          style={{ overflow: "visible" }}
        >
          <defs>
            <linearGradient id="routeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#2DD4BF" stopOpacity="0.9" />
              <stop offset="100%" stopColor="#14B8A6" stopOpacity="0.8" />
            </linearGradient>
            <filter id="glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>
          </defs>

          {/* Stylized continent silhouettes */}
          <g fill="#1E293B" opacity="0.65">
            {/* North America */}
            <path d="M 120 120 Q 220 100 310 160 Q 290 260 210 270 Q 150 220 120 120 Z" />
            {/* South America */}
            <path d="M 270 290 Q 340 310 330 410 Q 280 460 250 380 Z" />
            {/* Europe */}
            <path d="M 440 140 Q 520 120 540 190 Q 480 230 430 180 Z" />
            {/* Africa */}
            <path d="M 460 220 Q 550 230 540 360 Q 470 380 450 280 Z" />
            {/* Asia */}
            <path d="M 570 140 Q 780 130 840 230 Q 750 320 620 280 Q 580 200 570 140 Z" />
            {/* Australia */}
            <path d="M 780 340 Q 860 330 870 410 Q 800 430 780 340 Z" />
          </g>

          {/* Flight Route Curved Arcs */}
          {FLIGHT_ROUTES.map((route, i) => {
            const from = getHub(route.from);
            const to = getHub(route.to);
            if (!from || !to) return null;

            // Compute quadratic bezier curve midpoint
            const midX = (from.x + to.x) / 2;
            const midY = Math.min(from.y, to.y) - Math.abs(from.x - to.x) * 0.18;
            const isHighlight =
              selectedHub.id === from.id ||
              selectedHub.id === to.id ||
              hoveredHub?.id === from.id ||
              hoveredHub?.id === to.id;

            return (
              <g key={`route-${i}`}>
                <path
                  d={`M ${from.x} ${from.y} Q ${midX} ${midY} ${to.x} ${to.y}`}
                  fill="none"
                  stroke={isHighlight ? "#14B8A6" : "#334155"}
                  strokeWidth={isHighlight ? 2.5 : 1.2}
                  strokeDasharray={isHighlight ? "4 3" : "2 2"}
                  opacity={isHighlight ? 0.95 : 0.45}
                  className="transition-all duration-300"
                />
              </g>
            );
          })}

          {/* City Hub Markers */}
          {HUBS.map((hub) => {
            const isSelected = selectedHub.id === hub.id;
            const isHovered = hoveredHub?.id === hub.id;

            return (
              <g
                key={hub.id}
                className="cursor-pointer"
                onClick={() => setSelectedHub(hub)}
                onMouseEnter={() => setHoveredHub(hub)}
                onMouseLeave={() => setHoveredHub(null)}
              >
                {/* Pulsing ring for selected */}
                {isSelected && (
                  <circle
                    cx={hub.x}
                    cy={hub.y}
                    r="12"
                    fill="none"
                    stroke="#14B8A6"
                    strokeWidth="1.5"
                    className="animate-ping opacity-75"
                  />
                )}

                {/* Hub circle */}
                <circle
                  cx={hub.x}
                  cy={hub.y}
                  r={isSelected ? 6 : 4.5}
                  fill={isSelected ? "#14B8A6" : isHovered ? "#2DD4BF" : "#0D9488"}
                  stroke="#ffffff"
                  strokeWidth={isSelected ? 2 : 1}
                  filter={isSelected ? "url(#glow)" : undefined}
                />

                {/* Hub label */}
                <text
                  x={hub.x}
                  y={hub.y - 10}
                  textAnchor="middle"
                  fill={isSelected ? "#14B8A6" : "#E2E8F0"}
                  fontSize={isSelected ? "11" : "9.5"}
                  fontWeight={isSelected ? "bold" : "500"}
                  className="pointer-events-none drop-shadow"
                >
                  {hub.name}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Selected Hub Card Overlay */}
        <div className="mt-3 p-4 rounded-xl bg-white/10 backdrop-blur-md border border-white/15 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 text-white">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-135 from-[#14B8A6] to-[#0D9488] text-white flex items-center justify-center font-bold shadow-sm">
              <MapPin size={20} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <strong className="text-base font-bold text-white">
                  {selectedHub.name}, {selectedHub.country}
                </strong>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#14B8A6]/20 text-[#2DD4BF] border border-[#14B8A6]/30 font-semibold">
                  {selectedHub.tripsCount} journeys
                </span>
              </div>
              <p className="text-xs text-[#E2E8F0] flex items-center gap-3 mt-0.5">
                <span className="flex items-center gap-1">
                  <Clock size={12} /> Timezone: {selectedHub.timezone}
                </span>
                <span>·</span>
                <span>Avg flight from HQ: {selectedHub.avgDuration}</span>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs text-[#dbe7e4] bg-white/10 px-3 py-1.5 rounded-lg border border-white/10">
              Corporate Travel Approved Destination
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
