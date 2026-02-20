"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { staffApi, Staff } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent } from "@/components/ui/card";
import {
  Table, TableHeader, TableBody, TableRow, TableHead, TableCell,
} from "@/components/ui/table";
import {
  Dialog, DialogTrigger, DialogContent, DialogHeader, DialogTitle,
} from "@/components/ui/dialog";
import {
  Plus, Pencil, Trash2, Search, ChevronLeft, ChevronRight,
  ArrowUpDown, ArrowUp, ArrowDown, UserCheck, UserX, Users, UserMinus,
  X, Briefcase,
} from "lucide-react";

const emptyStaff: Staff = {
  firstName: "", lastName: "", email: "", phone: "", department: "", position: "", joinDate: "", active: true,
  salary: undefined, qualification: "", address: "",
};

const PAGE_SIZES = [10, 25, 50] as const;

type SortField = "firstName" | "email" | "department" | "position" | "phone" | "active";
type SortOrder = "asc" | "desc";
type StatusFilter = "all" | "true" | "false";

const SORTABLE_COLUMNS: { key: SortField; label: string }[] = [
  { key: "firstName", label: "Name" },
  { key: "email", label: "Email" },
  { key: "department", label: "Department" },
  { key: "position", label: "Position" },
  { key: "phone", label: "Phone" },
  { key: "active", label: "Status" },
];

const STATUS_FILTERS: { value: StatusFilter; label: string; icon: typeof Users }[] = [
  { value: "all", label: "All", icon: Users },
  { value: "true", label: "Active", icon: UserCheck },
  { value: "false", label: "Inactive", icon: UserMinus },
];

function getPageNumbers(current: number, total: number): (number | "...")[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i);
  }
  const pages: (number | "...")[] = [];
  pages.push(0);
  if (current > 3) pages.push("...");
  const start = Math.max(1, current - 1);
  const end = Math.min(total - 2, current + 1);
  for (let i = start; i <= end; i++) pages.push(i);
  if (current < total - 4) pages.push("...");
  pages.push(total - 1);
  return pages;
}

export default function StaffPage() {
  const queryClient = useQueryClient();
  const router = useRouter();
  const searchParams = useSearchParams();

  // URL-synced pagination state
  const search = searchParams.get("search") || "";
  const page = parseInt(searchParams.get("page") || "0", 10);
  const size = parseInt(searchParams.get("size") || "10", 10);
  const sortBy = (searchParams.get("sortBy") || "id") as SortField | "id";
  const order = (searchParams.get("order") || "asc") as SortOrder;
  const statusFilter = (searchParams.get("status") || "all") as StatusFilter;

  const updateParams = useCallback(
    (updates: Record<string, string | number | undefined>) => {
      const params = new URLSearchParams(searchParams.toString());
      Object.entries(updates).forEach(([key, value]) => {
        if (value === undefined || value === "") {
          params.delete(key);
        } else {
          params.set(key, String(value));
        }
      });
      if (params.get("size") === "10") params.delete("size");
      if (params.get("sortBy") === "id") params.delete("sortBy");
      if (params.get("order") === "asc") params.delete("order");
      if (params.get("page") === "0") params.delete("page");
      if (params.get("status") === "all") params.delete("status");
      router.push(`?${params.toString()}`, { scroll: false });
    },
    [searchParams, router]
  );

  const setSearch = useCallback(
    (value: string) => updateParams({ search: value || undefined, page: 0 }),
    [updateParams]
  );
  const setPage = useCallback(
    (value: number) => updateParams({ page: value }),
    [updateParams]
  );
  const setSize = useCallback(
    (value: number) => updateParams({ size: value, page: 0 }),
    [updateParams]
  );
  const setStatusFilter = useCallback(
    (value: StatusFilter) => updateParams({ status: value === "all" ? undefined : value, page: 0 }),
    [updateParams]
  );
  const toggleSort = useCallback(
    (field: SortField) => {
      if (sortBy === field) {
        updateParams({ order: order === "asc" ? "desc" : "asc", page: 0 });
      } else {
        updateParams({ sortBy: field, order: "asc", page: 0 });
      }
    },
    [sortBy, order, updateParams]
  );

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Staff | null>(null);
  const [form, setForm] = useState<Staff>(emptyStaff);
  const [formError, setFormError] = useState("");

  // Selection state
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const selectAllRef = useRef<HTMLInputElement>(null);

  // Clear selection when page/search/filter changes
  useEffect(() => {
    setSelectedIds(new Set());
  }, [page, search, statusFilter, size, sortBy, order]);

  const activeParam = statusFilter === "all" ? undefined : statusFilter === "true";

  const { data, isLoading } = useQuery({
    queryKey: ["staff", search, page, size, sortBy, order, statusFilter],
    queryFn: () =>
      staffApi.getAll({ search: search || undefined, page, size, sortBy, order, active: activeParam }).then((r) => r.data),
  });

  // Derived selection info
  const selectedStaff = data?.content.filter((s) => selectedIds.has(s.id!)) ?? [];
  const hasSelection = selectedIds.size > 0;
  const allOnPageSelected = (data?.content.length ?? 0) > 0 && data?.content.every((s) => selectedIds.has(s.id!));
  const someSelected = hasSelection && !allOnPageSelected;
  const anySelectedInactive = selectedStaff.some((s) => s.active === false);
  const anySelectedActive = selectedStaff.some((s) => s.active !== false);

  // Update indeterminate state on the select-all checkbox
  useEffect(() => {
    if (selectAllRef.current) {
      selectAllRef.current.indeterminate = someSelected;
    }
  }, [someSelected]);

  const toggleSelectAll = () => {
    if (allOnPageSelected) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(data?.content.map((s) => s.id!) ?? []));
    }
  };

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  // Keyboard navigation
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement).tagName;
      if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT" || dialogOpen) return;
      if (e.key === "ArrowLeft" && page > 0) setPage(page - 1);
      else if (e.key === "ArrowRight" && data && page < data.totalPages - 1) setPage(page + 1);
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [page, data, dialogOpen, setPage]);

  const invalidateAndClear = () => {
    queryClient.invalidateQueries({ queryKey: ["staff"] });
    setSelectedIds(new Set());
  };

  const createMutation = useMutation({
    mutationFn: (s: Staff) => staffApi.create(s),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["staff"] }); closeDialog(); },
    onError: (err: any) => setFormError(err.response?.data?.error || "Failed to create"),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: Staff }) => staffApi.update(id, data),
    onSuccess: () => { queryClient.invalidateQueries({ queryKey: ["staff"] }); closeDialog(); },
    onError: (err: any) => setFormError(err.response?.data?.error || "Failed to update"),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => staffApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["staff"] }),
  });

  const toggleActiveMutation = useMutation({
    mutationFn: (id: number) => staffApi.toggleActive(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["staff"] }),
  });

  // Bulk mutations
  const bulkDeleteMutation = useMutation({
    mutationFn: (ids: number[]) => staffApi.bulkDelete(ids),
    onSuccess: invalidateAndClear,
  });

  const bulkActivateMutation = useMutation({
    mutationFn: (ids: number[]) => staffApi.bulkActivate(ids),
    onSuccess: invalidateAndClear,
  });

  const bulkDeactivateMutation = useMutation({
    mutationFn: (ids: number[]) => staffApi.bulkDeactivate(ids),
    onSuccess: invalidateAndClear,
  });

  const isBulkPending = bulkDeleteMutation.isPending || bulkActivateMutation.isPending
    || bulkDeactivateMutation.isPending;

  const handleBulkDelete = () => {
    if (confirm(`Are you sure you want to delete ${selectedIds.size} staff member(s)? This cannot be undone.`)) {
      bulkDeleteMutation.mutate(Array.from(selectedIds));
    }
  };

  const openCreate = () => { setEditing(null); setForm(emptyStaff); setFormError(""); setDialogOpen(true); };
  const openEdit = (s: Staff) => { setEditing(s); setForm({ ...s }); setFormError(""); setDialogOpen(true); };
  const closeDialog = () => { setDialogOpen(false); setEditing(null); setForm(emptyStaff); setFormError(""); };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const submitData = {
      ...form,
      salary: form.salary ? Number(form.salary) : undefined,
    };
    if (editing?.id) {
      updateMutation.mutate({ id: editing.id, data: submitData });
    } else {
      createMutation.mutate(submitData);
    }
  };

  const handleDelete = (id: number) => {
    if (confirm("Are you sure you want to delete this staff member?")) {
      deleteMutation.mutate(id);
    }
  };

  const setField = (field: keyof Staff, value: string | number | undefined) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const SortIcon = ({ field }: { field: SortField }) => {
    if (sortBy !== field) return <ArrowUpDown className="h-3 w-3 ml-1 opacity-40" />;
    return order === "asc"
      ? <ArrowUp className="h-3 w-3 ml-1" />
      : <ArrowDown className="h-3 w-3 ml-1" />;
  };

  return (
    <div className="space-y-6 pb-20">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Staff</h1>
          <p className="text-sm text-muted-foreground">Manage your staff records</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button onClick={openCreate} size="lg">
              <Plus className="h-4 w-4 mr-2" /> Add Staff
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>{editing ? "Edit Staff" : "Add Staff"}</DialogTitle>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>First Name <span className="text-destructive">*</span></Label>
                  <Input value={form.firstName} onChange={(e) => setField("firstName", e.target.value)} required />
                </div>
                <div className="space-y-2">
                  <Label>Last Name <span className="text-destructive">*</span></Label>
                  <Input value={form.lastName} onChange={(e) => setField("lastName", e.target.value)} required />
                </div>
              </div>
              <div className="space-y-2">
                <Label>Email <span className="text-destructive">*</span></Label>
                <Input type="email" value={form.email} onChange={(e) => setField("email", e.target.value)} required />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Phone</Label>
                  <Input value={form.phone || ""} onChange={(e) => setField("phone", e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>Department</Label>
                  <Input value={form.department || ""} onChange={(e) => setField("department", e.target.value)} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Position</Label>
                  <Input value={form.position || ""} onChange={(e) => setField("position", e.target.value)} />
                </div>
                <div className="space-y-2">
                  <Label>Join Date</Label>
                  <Input type="date" value={form.joinDate || ""} onChange={(e) => setField("joinDate", e.target.value)} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Salary</Label>
                  <Input type="number" step="0.01" value={form.salary ?? ""} onChange={(e) => setField("salary", e.target.value ? Number(e.target.value) : undefined)} />
                </div>
                <div className="space-y-2">
                  <Label>Qualification</Label>
                  <Input value={form.qualification || ""} onChange={(e) => setField("qualification", e.target.value)} />
                </div>
              </div>
              <div className="space-y-2">
                <Label>Address</Label>
                <Input value={form.address || ""} onChange={(e) => setField("address", e.target.value)} />
              </div>
              {formError && <p className="text-sm text-destructive">{formError}</p>}
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={closeDialog}>Cancel</Button>
                <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
                  {editing ? "Update" : "Create"}
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-muted-foreground">Total Staff</p>
                <p className="text-3xl font-bold">{data?.totalElements ?? 0}</p>
              </div>
              <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                <Briefcase className="h-5 w-5 text-primary" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Toolbar */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col lg:flex-row gap-4 items-start lg:items-center">
            <div className="flex items-center gap-1 p-1 bg-muted rounded-lg shrink-0">
              {STATUS_FILTERS.map((opt) => {
                const Icon = opt.icon;
                return (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setStatusFilter(opt.value)}
                    className={`inline-flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-all ${
                      statusFilter === opt.value
                        ? "bg-background text-foreground shadow-sm"
                        : "text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                    {opt.label}
                  </button>
                );
              })}
            </div>
            <div className="flex-1 flex justify-center">
              <div className="relative w-full lg:w-80">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by name, email or department..."
                  className="pl-10"
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                />
              </div>
            </div>
            {hasSelection && (
              <div className="flex items-center gap-2 shrink-0">
                <span className="text-sm font-medium text-muted-foreground mr-1">
                  {selectedIds.size} selected
                </span>
                <button
                  type="button"
                  onClick={() => setSelectedIds(new Set())}
                  className="text-muted-foreground hover:text-foreground mr-1"
                  title="Clear selection"
                >
                  <X className="h-4 w-4" />
                </button>
                {anySelectedInactive && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="text-emerald-600 border-emerald-200 hover:bg-emerald-50"
                    disabled={isBulkPending}
                    onClick={() => bulkActivateMutation.mutate(Array.from(selectedIds))}
                  >
                    <UserCheck className="h-4 w-4 mr-1.5" />
                    Activate
                  </Button>
                )}
                {anySelectedActive && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="text-orange-600 border-orange-200 hover:bg-orange-50"
                    disabled={isBulkPending}
                    onClick={() => bulkDeactivateMutation.mutate(Array.from(selectedIds))}
                  >
                    <UserX className="h-4 w-4 mr-1.5" />
                    Deactivate
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="outline"
                  className="text-destructive border-red-200 hover:bg-red-50"
                  disabled={isBulkPending}
                  onClick={handleBulkDelete}
                >
                  <Trash2 className="h-4 w-4 mr-1.5" />
                  Delete
                </Button>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Table */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-12">
                  <input
                    ref={selectAllRef}
                    type="checkbox"
                    checked={allOnPageSelected && (data?.content.length ?? 0) > 0}
                    onChange={toggleSelectAll}
                    className="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
                    title="Select all on this page"
                  />
                </TableHead>
                {SORTABLE_COLUMNS.map((col) => (
                  <TableHead
                    key={col.key}
                    className="cursor-pointer select-none hover:bg-muted/50 transition-colors"
                    onClick={() => toggleSort(col.key)}
                  >
                    <div className="flex items-center">
                      {col.label}
                      <SortIcon field={col.key} />
                    </div>
                  </TableHead>
                ))}
                <TableHead className="w-28 text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-12 text-muted-foreground">
                    <div className="flex flex-col items-center gap-2">
                      <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                      <span>Loading staff...</span>
                    </div>
                  </TableCell>
                </TableRow>
              ) : data?.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-12 text-muted-foreground">
                    <div className="flex flex-col items-center gap-2">
                      <Briefcase className="h-8 w-8 opacity-40" />
                      <span>No staff found</span>
                      {statusFilter !== "all" && (
                        <Button variant="link" size="sm" onClick={() => setStatusFilter("all")}>
                          Clear filter
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ) : (
                data?.content.map((s) => (
                  <TableRow
                    key={s.id}
                    className={`${s.active === false ? "opacity-60" : ""} ${selectedIds.has(s.id!) ? "bg-primary/5" : ""}`}
                  >
                    <TableCell>
                      <input
                        type="checkbox"
                        checked={selectedIds.has(s.id!)}
                        onChange={() => toggleSelect(s.id!)}
                        className="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
                      />
                    </TableCell>
                    <TableCell className="font-medium">{s.firstName} {s.lastName}</TableCell>
                    <TableCell className="text-muted-foreground">{s.email}</TableCell>
                    <TableCell>{s.department || "-"}</TableCell>
                    <TableCell>{s.position || "-"}</TableCell>
                    <TableCell>{s.phone || "-"}</TableCell>
                    <TableCell>
                      <span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                        s.active !== false
                          ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                          : "bg-red-50 text-red-700 border border-red-200"
                      }`}>
                        <span className={`h-1.5 w-1.5 rounded-full ${
                          s.active !== false ? "bg-emerald-500" : "bg-red-500"
                        }`} />
                        {s.active !== false ? "Active" : "Inactive"}
                      </span>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-1 justify-end">
                        <Button
                          variant="ghost"
                          size="icon"
                          title={s.active !== false ? "Deactivate" : "Activate"}
                          onClick={() => toggleActiveMutation.mutate(s.id!)}
                          disabled={toggleActiveMutation.isPending}
                        >
                          {s.active !== false
                            ? <UserX className="h-4 w-4 text-orange-500" />
                            : <UserCheck className="h-4 w-4 text-emerald-600" />
                          }
                        </Button>
                        <Button variant="ghost" size="icon" title="Edit" onClick={() => openEdit(s)}>
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" title="Delete" onClick={() => handleDelete(s.id!)}>
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>

        {/* Pagination */}
        {data && (
          <div className="border-t px-6 py-4">
            <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                <span>
                  {data.totalElements === 0
                    ? "No results"
                    : `Showing ${page * size + 1}\u2013${Math.min((page + 1) * size, data.totalElements)} of ${data.totalElements}`}
                </span>
                <div className="flex items-center gap-2">
                  <span>Rows per page:</span>
                  <select
                    value={size}
                    onChange={(e) => setSize(Number(e.target.value))}
                    title="Rows per page"
                    className="h-8 rounded-md border border-input bg-background px-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                  >
                    {PAGE_SIZES.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
              </div>

              {data.totalPages > 1 && (
                <div className="flex items-center gap-1">
                  <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)} aria-label="Previous page">
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  {getPageNumbers(page, data.totalPages).map((p, i) =>
                    p === "..." ? (
                      <span key={`ellipsis-${i}`} className="px-2 text-sm text-muted-foreground">...</span>
                    ) : (
                      <Button key={p} variant={p === page ? "default" : "outline"} size="sm" className="min-w-[36px]" onClick={() => setPage(p)}>
                        {p + 1}
                      </Button>
                    )
                  )}
                  <Button variant="outline" size="sm" disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)} aria-label="Next page">
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              )}
            </div>
          </div>
        )}
      </Card>

    </div>
  );
}
