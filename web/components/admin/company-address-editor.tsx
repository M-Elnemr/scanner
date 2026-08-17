"use client";

import { Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import type { CompanyAddressInput } from "@/lib/admin/companies-actions";
import type { CityOption } from "@/lib/admin/cities";

export function CompanyAddressEditor({
  addresses,
  onChange,
  cities,
}: {
  addresses: CompanyAddressInput[];
  onChange: (addresses: CompanyAddressInput[]) => void;
  cities: CityOption[];
}) {
  function update(index: number, patch: Partial<CompanyAddressInput>) {
    onChange(addresses.map((a, i) => (i === index ? { ...a, ...patch } : a)));
  }

  function remove(index: number) {
    onChange(addresses.filter((_, i) => i !== index));
  }

  function add() {
    onChange([...addresses, { cityId: cities[0]?.id ?? "", addressText: "", mobileNumber: "" }]);
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <Label>Branch addresses</Label>
        <Button type="button" variant="outline" size="sm" onClick={add}>
          <Plus className="size-3.5" /> Add branch
        </Button>
      </div>

      {addresses.length === 0 && (
        <p className="rounded-lg border border-dashed border-border p-4 text-sm text-muted-foreground">
          At least one branch address is required.
        </p>
      )}

      <div className="space-y-3">
        {addresses.map((address, index) => (
          <div key={index} className="grid grid-cols-1 gap-2 rounded-lg border border-border p-3 sm:grid-cols-[1fr_1fr_auto]">
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">City</Label>
              <Select value={address.cityId} onValueChange={(v) => v && update(index, { cityId: v })}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Select city" />
                </SelectTrigger>
                <SelectContent>
                  {cities.map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground">Mobile number</Label>
              <Input value={address.mobileNumber} onChange={(e) => update(index, { mobileNumber: e.target.value })} />
            </div>
            <div className="flex items-end sm:col-span-1">
              <Button type="button" variant="ghost" size="icon" className="text-destructive" onClick={() => remove(index)}>
                <Trash2 className="size-4" />
              </Button>
            </div>
            <div className="space-y-1 sm:col-span-3">
              <Label className="text-xs text-muted-foreground">Address</Label>
              <Input value={address.addressText} onChange={(e) => update(index, { addressText: e.target.value })} />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
