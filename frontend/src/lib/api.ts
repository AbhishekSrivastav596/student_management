import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (error) => {
    // Only redirect on 401 for protected routes — not for auth endpoints where
    // 401 means bad credentials and the component handles the error itself.
    if (error.response?.status === 401 && !error.config?.url?.includes("/auth/")) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export default api;

// Auth
export const authApi = {
  login: (data: { email: string; password: string }) =>
    api.post("/auth/login", data),
  register: (data: { name: string; email: string; password: string }) =>
    api.post("/auth/register", data),
};

// Students
export interface Student {
  id?: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  studentClass?: string;
  section?: string;
  enrollmentDate?: string;
  active?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export const studentApi = {
  getAll: (params: {
    search?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    order?: "asc" | "desc";
    active?: boolean;
  }) => {
    // Strip empty/undefined values to avoid sending search= (empty) which can cause 400
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([, v]) => v !== undefined && v !== "")
    );
    return api.get<PageResponse<Student>>("/students", { params: cleanParams });
  },
  getById: (id: number) => api.get<Student>(`/students/${id}`),
  create: (data: Student) => api.post<Student>("/students", data),
  update: (id: number, data: Student) =>
    api.put<Student>(`/students/${id}`, data),
  delete: (id: number) => api.delete(`/students/${id}`),
  toggleActive: (id: number) => api.patch<Student>(`/students/${id}/toggle-active`),
  bulkDelete: (ids: number[]) => api.post("/students/bulk/delete", { ids }),
  bulkActivate: (ids: number[]) => api.post("/students/bulk/activate", { ids }),
  bulkDeactivate: (ids: number[]) => api.post("/students/bulk/deactivate", { ids }),
  bulkSendInvite: (ids: number[]) => api.post("/students/bulk/send-invite", { ids }),
};
