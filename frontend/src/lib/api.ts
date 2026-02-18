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
    if (error.response?.status === 401) {
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
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export const studentApi = {
  getAll: (params: { search?: string; page?: number; size?: number }) =>
    api.get<PageResponse<Student>>("/students", { params }),
  getById: (id: number) => api.get<Student>(`/students/${id}`),
  create: (data: Student) => api.post<Student>("/students", data),
  update: (id: number, data: Student) =>
    api.put<Student>(`/students/${id}`, data),
  delete: (id: number) => api.delete(`/students/${id}`),
};
