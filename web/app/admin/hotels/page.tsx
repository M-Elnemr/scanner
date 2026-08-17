import type { Metadata } from "next";
import { Plus, MapPin } from "lucide-react";
import { apiClient } from "@/lib/auth/server";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { HotelFormDialog } from "@/components/admin/hotel-form-dialog";
import { HotelDeleteButton } from "@/components/admin/hotel-delete-button";

export const metadata: Metadata = { title: "Hotels · Admin" };

const CITY_LABEL: Record<string, string> = { MAKKAH: "Makkah", MADINAH: "Madinah" };

export default async function AdminHotelsPage() {
  const api = await apiClient();
  const result = await api.GET("/api/v1/admin/hotels", { cache: "no-store" });
  const hotels = result.data?.data ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">Hotels</h1>
          <p className="text-sm text-muted-foreground">{hotels.length} in the catalogue</p>
        </div>
        <HotelFormDialog
          mode="create"
          trigger={
            <Button>
              <Plus /> Add hotel
            </Button>
          }
        />
      </div>

      <div className="overflow-hidden rounded-xl border border-border bg-background">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Hotel</TableHead>
              <TableHead>City</TableHead>
              <TableHead>Stars</TableHead>
              <TableHead>Distance</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="w-24" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {hotels.length === 0 && (
              <TableRow>
                <TableCell colSpan={6} className="py-10 text-center text-muted-foreground">
                  No hotels yet — add the first one to the catalogue.
                </TableCell>
              </TableRow>
            )}
            {hotels.map((h) => (
              <TableRow key={h.id}>
                <TableCell className="font-medium">{h.name}</TableCell>
                <TableCell className="text-muted-foreground">{h.city ? CITY_LABEL[h.city] : "—"}</TableCell>
                <TableCell className="text-muted-foreground">{h.stars ? `${h.stars}★` : "—"}</TableCell>
                <TableCell className="text-muted-foreground">
                  {h.distanceToHaramM != null ? (
                    <span className="inline-flex items-center gap-1">
                      <MapPin className="size-3" /> {h.distanceToHaramM}m{h.canWalk ? " · walkable" : ""}
                    </span>
                  ) : (
                    "—"
                  )}
                </TableCell>
                <TableCell>
                  <Badge variant={h.active ? "default" : "outline"}>{h.active ? "Active" : "Inactive"}</Badge>
                </TableCell>
                <TableCell>
                  <div className="flex items-center justify-end gap-1">
                    <HotelFormDialog
                      mode="edit"
                      hotelId={h.id}
                      initial={{
                        city: h.city ?? "MAKKAH",
                        name: h.name ?? "",
                        nameAr: h.nameAr ?? "",
                        stars: h.stars != null ? String(h.stars) : "",
                        distanceToHaramM: h.distanceToHaramM != null ? String(h.distanceToHaramM) : "",
                        canWalk: h.canWalk ?? false,
                        locationUrl: h.locationUrl ?? "",
                        active: h.active ?? true,
                      }}
                      trigger={
                        <Button variant="ghost" size="sm">
                          Edit
                        </Button>
                      }
                    />
                    <HotelDeleteButton id={h.id ?? ""} />
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
