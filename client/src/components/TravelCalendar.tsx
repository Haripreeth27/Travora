import { useState, useMemo } from "react";
import {
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
  Plane,
  Clock,
  ArrowRight,
  Sparkles,
  MapPin,
  CalendarDays,
  X,
} from "lucide-react";
import type { Journey } from "../types";

interface TravelCalendarProps {
  journeys: Journey[];
  onSelectJourney?: (journey: Journey) => void;
}

export function TravelCalendar({ journeys, onSelectJourney }: TravelCalendarProps) {
  const [currentDate, setCurrentDate] = useState(() => new Date());
  const [selectedDayJourneys, setSelectedDayJourneys] = useState<{ date: string; trips: Journey[] } | null>(null);

  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  ];

  // Parse journeys to date keys
  const journeyDateMap = useMemo(() => {
    const map = new Map<string, Journey[]>();
    for (const journey of journeys) {
      // dates is e.g. "12 Oct 2026" or "12 Oct 2026 – 16 Oct 2026"
      const dateParts = journey.dates.split("–").map((s) => s.trim());
      const startDate = new Date(dateParts[0]);
      if (!Number.isNaN(startDate.getTime())) {
        const key = `${startDate.getFullYear()}-${String(startDate.getMonth() + 1).padStart(2, "0")}-${String(startDate.getDate()).padStart(2, "0")}`;
        const existing = map.get(key) || [];
        existing.push(journey);
        map.set(key, existing);
      }
    }
    return map;
  }, [journeys]);

  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const firstDayIndex = new Date(year, month, 1).getDay();

  const prevMonth = () => {
    setCurrentDate(new Date(year, month - 1, 1));
  };

  const nextMonth = () => {
    setCurrentDate(new Date(year, month + 1, 1));
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  // Calendar stats for this month
  const thisMonthTrips = useMemo(() => {
    let count = 0;
    for (let day = 1; day <= daysInMonth; day++) {
      const key = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
      if (journeyDateMap.has(key)) {
        count += (journeyDateMap.get(key)?.length || 0);
      }
    }
    return count;
  }, [journeyDateMap, year, month, daysInMonth]);

  const todayStr = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  }, []);

  return (
    <div className="space-y-6">
      {/* Calendar Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-5 rounded-2xl bg-white dark:bg-[#1a2c30] border border-[#e2e8e5] dark:border-[#2b444a] shadow-sm">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-[#398064]/10 text-[#398064] flex items-center justify-center">
            <CalendarDays size={20} />
          </div>
          <div>
            <h2 className="text-xl font-bold tracking-tight text-[#182329] dark:text-[#edf5f1]">
              {monthNames[month]} {year}
            </h2>
            <p className="text-xs text-[#839099]">
              {thisMonthTrips} scheduled {thisMonthTrips === 1 ? "journey" : "journeys"} in this month
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={goToToday}
            className="px-3 py-1.5 text-xs font-semibold rounded-lg bg-[#f5f8f6] dark:bg-[#243b40] text-[#182329] dark:text-[#dbe7e4] hover:bg-[#e2e8e5] dark:hover:bg-[#2e4c52] transition-colors"
          >
            Today
          </button>
          <div className="flex items-center border border-[#e2e8e5] dark:border-[#2b444a] rounded-lg overflow-hidden">
            <button
              type="button"
              onClick={prevMonth}
              className="p-1.5 text-[#52616a] dark:text-[#9bb0ac] hover:bg-[#f5f8f6] dark:hover:bg-[#243b40] transition-colors"
              aria-label="Previous month"
            >
              <ChevronLeft size={18} />
            </button>
            <button
              type="button"
              onClick={nextMonth}
              className="p-1.5 text-[#52616a] dark:text-[#9bb0ac] hover:bg-[#f5f8f6] dark:hover:bg-[#243b40] transition-colors"
              aria-label="Next month"
            >
              <ChevronRight size={18} />
            </button>
          </div>
        </div>
      </div>

      {/* Calendar Grid */}
      <div className="rounded-2xl bg-white dark:bg-[#1a2c30] border border-[#e2e8e5] dark:border-[#2b444a] overflow-hidden shadow-sm">
        {/* Day of Week Headers */}
        <div className="grid grid-cols-7 border-b border-[#e2e8e5] dark:border-[#2b444a] bg-[#f7faf8] dark:bg-[#16272a] text-center text-xs font-bold text-[#839099] uppercase tracking-wider py-3">
          <span>Sun</span>
          <span>Mon</span>
          <span>Tue</span>
          <span>Wed</span>
          <span>Thu</span>
          <span>Fri</span>
          <span>Sat</span>
        </div>

        {/* Calendar Days */}
        <div className="grid grid-cols-7 auto-rows-fr gap-px bg-[#e2e8e5] dark:bg-[#203638]">
          {/* Empty cells before month starts */}
          {Array.from({ length: firstDayIndex }).map((_, index) => (
            <div key={`empty-${index}`} className="min-h-[96px] bg-[#fbfcfb] dark:bg-[#152326]/50 p-2" />
          ))}

          {/* Month days */}
          {Array.from({ length: daysInMonth }).map((_, index) => {
            const day = index + 1;
            const dateKey = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
            const isToday = dateKey === todayStr;
            const trips = journeyDateMap.get(dateKey) || [];

            return (
              <div
                key={dateKey}
                onClick={() => {
                  if (trips.length > 0) {
                    setSelectedDayJourneys({ date: dateKey, trips });
                  }
                }}
                className={`min-h-[96px] bg-white dark:bg-[#1a2c30] p-2 transition-all flex flex-col justify-between ${
                  trips.length > 0
                    ? "cursor-pointer hover:bg-[#f5f8f6] dark:hover:bg-[#203638]"
                    : ""
                }`}
              >
                <div className="flex items-center justify-between">
                  <span
                    className={`inline-flex items-center justify-center text-xs font-semibold w-6 h-6 rounded-full ${
                      isToday
                        ? "bg-[#e7a947] text-[#182329] font-bold"
                        : "text-[#182329] dark:text-[#edf5f1]"
                    }`}
                  >
                    {day}
                  </span>
                  {trips.length > 0 && (
                    <span className="text-[10px] font-bold px-1.5 py-0.5 rounded-full bg-[#398064]/15 text-[#398064] dark:text-[#6ee7b7]">
                      {trips.length} {trips.length === 1 ? "trip" : "trips"}
                    </span>
                  )}
                </div>

                {/* Trip Badges on Day */}
                <div className="space-y-1 mt-1">
                  {trips.slice(0, 2).map((trip) => {
                    const statusColors =
                      trip.status === "Approved" || trip.status === "Booked"
                        ? "bg-[#398064]/15 text-[#398064] dark:text-[#6ee7b7] border-[#398064]/30"
                        : "bg-[#e7a947]/15 text-[#b47e24] dark:text-[#fcd34d] border-[#e7a947]/30";

                    return (
                      <div
                        key={trip.id}
                        onClick={(e) => {
                          e.stopPropagation();
                          if (onSelectJourney) onSelectJourney(trip);
                        }}
                        className={`text-[10px] font-medium px-1.5 py-1 rounded border truncate flex items-center gap-1 ${statusColors} hover:opacity-80 transition-opacity`}
                        title={`${trip.destination} (${trip.status})`}
                      >
                        <Plane size={10} className="shrink-0" />
                        <span className="truncate">{trip.destination}</span>
                      </div>
                    );
                  })}
                  {trips.length > 2 && (
                    <span className="text-[9px] text-[#839099] block text-center">
                      +{trips.length - 2} more
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Selected Day Journeys Modal */}
      {selectedDayJourneys && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm animate-in fade-in"
          onClick={() => setSelectedDayJourneys(null)}
        >
          <div
            className="w-full max-w-md bg-white dark:bg-[#1a2c30] rounded-2xl shadow-xl border border-[#e2e8e5] dark:border-[#2b444a] p-6 space-y-4"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-[#e2e8e5] dark:border-[#2b444a] pb-3">
              <div>
                <span className="text-[10px] font-bold uppercase tracking-wider text-[#e7a947]">
                  SCHEDULED TRAVEL
                </span>
                <h3 className="text-lg font-bold text-[#182329] dark:text-[#edf5f1]">
                  Trips on {selectedDayJourneys.date}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setSelectedDayJourneys(null)}
                className="text-[#839099] hover:text-[#182329] dark:hover:text-white"
                aria-label="Close"
              >
                <X size={16} />
              </button>
            </div>

            <div className="space-y-3 max-h-[300px] overflow-y-auto">
              {selectedDayJourneys.trips.map((trip) => (
                <div
                  key={trip.id}
                  className="p-3.5 rounded-xl border border-[#e2e8e5] dark:border-[#2b444a] bg-[#f7faf8] dark:bg-[#152528] space-y-2 hover:border-[#398064] transition-colors cursor-pointer"
                  onClick={() => {
                    setSelectedDayJourneys(null);
                    if (onSelectJourney) onSelectJourney(trip);
                  }}
                >
                  <div className="flex items-center justify-between">
                    <strong className="text-sm text-[#182329] dark:text-[#edf5f1]">
                      {trip.destination}
                    </strong>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#398064]/20 text-[#398064] dark:text-[#6ee7b7]">
                      {trip.status}
                    </span>
                  </div>
                  <div className="flex items-center gap-2 text-xs text-[#52616a] dark:text-[#9bb0ac]">
                    <span>{trip.from}</span>
                    <ArrowRight size={12} />
                    <span>{trip.to}</span>
                  </div>
                  <div className="text-[11px] text-[#839099]">
                    Project: {trip.project} · {trip.tripType}
                  </div>
                </div>
              ))}
            </div>

            <button
              type="button"
              onClick={() => setSelectedDayJourneys(null)}
              className="w-full py-2.5 rounded-xl bg-[#398064] text-white font-semibold text-sm hover:bg-[#2e6852] transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
