'use client';

import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { AdminOnly } from '@/lib/components/protected-route';
import { useAuth } from '@/lib/context/auth-context';
import {
  getTablesList,
  getMenuItems,
  getOrders,
  getCategories,
  getUsersList,
  getBookings,
  getAdminDashboardSummary,
  getRecentOrders,
  getPendingReservations,
  approveReservation,
  rejectReservation,
  updateUser,
  deleteUser,
  register,
  updateOrderStatus,
  createMenuItem,
  updateMenuItem,
  deleteMenuItem,
  updateTableStatus,
  sendQRCodeToESP32,
  createCategory,
  updateCategory,
  deleteCategory,
  createTable,
  updateTable,
  deleteTable
} from '@/lib/api';
import type {
  RestaurantTable,
  MenuItem,
  Order,
  Category,
  User,
  Booking,
  AdminDashboardSummary,
  RecentOrder
} from '@/lib/types';
import {
  LogOut,
  Users,
  ChefHat,
  ShoppingCart,
  Table,
  Package,
  Calendar,
  BarChart3,
  Settings,
  TrendingUp,
  DollarSign,
  Clock,
  CheckCircle,
  XCircle,
  Edit,
  Plus,
  QrCode,
  Trash2,
  Menu
} from 'lucide-react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';

function AdminDashboardContent() {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState("overview");

  // State for dashboard data
  const [tables, setTables] = useState<RestaurantTable[]>([]);
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const totalItems = menuItems.length;
  // Admin dashboard state
  const [dashboardSummary, setDashboardSummary] = useState<AdminDashboardSummary | null>(null);
  const [recentOrders, setRecentOrders] = useState<RecentOrder[]>([]);
  const [pendingReservations, setPendingReservations] = useState<Booking[]>([]);
  const [loadingDashboard, setLoadingDashboard] = useState(true);
  
  // Orders state
  const [orders, setOrders] = useState<Order[]>([]);
  
  // Dialog states
  const [userDialogOpen, setUserDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [menuDialogOpen, setMenuDialogOpen] = useState(false);
  const [selectedMenuItem, setSelectedMenuItem] = useState<MenuItem | null>(null);
  
  // Form states
  const [userForm, setUserForm] = useState({ username: '', password: '', fullName: '', email: '', phone: '', role: 'CUSTOMER', status: 'ACTIVE' });
  const [menuForm, setMenuForm] = useState({ name: '', description: '', price: '', categoryId: '', imageUrl: '', isAvailable: true });

  // Category state
  const [categoryDialogOpen, setCategoryDialogOpen] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<Category | null>(null);
  const [categoryForm, setCategoryForm] = useState({ name: '', description: '' });

  // Table state
  const [tableDialogOpen, setTableDialogOpen] = useState(false);
  const [selectedTableForEdit, setSelectedTableForEdit] = useState<RestaurantTable | null>(null);
  const [tableForm, setTableForm] = useState({ tableName: '', capacity: '', status: 'AVAILABLE', tableType: 'INDOOR', location: '' });

  useEffect(() => {
    loadDashboardData();
    loadOverviewData();
    
    // Auto refresh dashboard data every 30 seconds
    const interval = setInterval(() => {
      loadOverviewData();
    }, 30000);
    
    return () => clearInterval(interval);
  }, []);

  const loadOverviewData = async () => {
    try {
      setLoadingDashboard(true);
      const [summaryRes, ordersRes, reservationsRes] = await Promise.all([
        getAdminDashboardSummary(),
        getRecentOrders(5),
        getPendingReservations(),
      ]);

      setDashboardSummary(summaryRes);
      
      // Sắp xếp recentOrders: đơn mới nhất lên trên
      const sortedRecentOrders = (ordersRes.orders || []).sort((a, b) => {
        const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return dateB - dateA; // DESC: mới nhất lên trên
      });
      setRecentOrders(sortedRecentOrders);
      
      // Sắp xếp pendingReservations: đặt bàn mới nhất lên trên
      const sortedReservations = (reservationsRes.reservations || []).sort((a, b) => {
        const dateA = a.date ? new Date(a.date).getTime() : 0;
        const dateB = b.date ? new Date(b.date).getTime() : 0;
        return dateB - dateA; // DESC: mới nhất lên trên
      });
      setPendingReservations(sortedReservations);
    } catch (error: any) {
      // Handle 401/403 - redirect to login
      if (error.message?.includes('401') || error.message?.includes('403') || error.message?.includes('Authentication')) {
        toast.error('Phiên đăng nhập đã hết hạn');
        logout();
        router.push('/login');
        return;
      }
      toast.error('Không thể tải dữ liệu tổng quan');
    } finally {
      setLoadingDashboard(false);
    }
  };

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [
        tablesRes,
        menuRes,
        categoriesRes,
        usersRes,
        ordersRes
      ] = await Promise.all([
        getTablesList(),
        getMenuItems(),
        getCategories(),
        getUsersList(),
        getOrders(),
      ]);

      setTables(tablesRes.tables || []);
      setMenuItems(menuRes.menuItems || []);
      setCategories(categoriesRes.categories || []);
      setUsers(usersRes.users || []);
      
      // Sắp xếp orders: đơn mới nhất lên trên (theo createdAt DESC)
      const sortedOrders = (ordersRes.orders || []).sort((a, b) => {
        const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return dateB - dateA; // DESC: mới nhất lên trên
      });
      setOrders(sortedOrders);
    } catch (error: any) {
      if (error.message?.includes('401') || error.message?.includes('403')) {
        toast.error('Phiên đăng nhập đã hết hạn');
        logout();
        router.push('/login');
        return;
      }
      toast.error('Không thể tải dữ liệu dashboard');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveReservation = async (id: number) => {
    try {
      await approveReservation(id);
      toast.success('Đã duyệt đặt bàn');
      await loadOverviewData();
    } catch (error: any) {
      if (error.message?.includes('401') || error.message?.includes('403')) {
        toast.error('Phiên đăng nhập đã hết hạn');
        logout();
        router.push('/login');
        return;
      }
      toast.error('Không thể duyệt đặt bàn');
    }
  };

  const handleRejectReservation = async (id: number) => {
    try {
      await rejectReservation(id);
      toast.success('Đã từ chối đặt bàn');
      await loadOverviewData();
    } catch (error: any) {
      if (error.message?.includes('401') || error.message?.includes('403')) {
        toast.error('Phiên đăng nhập đã hết hạn');
        logout();
        router.push('/login');
        return;
      }
      toast.error('Không thể từ chối đặt bàn');
    }
  };

  const handleLogout = () => {
    logout();
    toast.success('Đã đăng xuất');
    router.push('/');
  };

  // User management handlers
  const handleAddUser = () => {
    setSelectedUser(null);
    setUserForm({ username: '', password: '', fullName: '', email: '', phone: '', role: 'CUSTOMER', status: 'ACTIVE' });
    setUserDialogOpen(true);
  };

  const handleEditUser = (user: User) => {
    setSelectedUser(user);
    setUserForm({
      username: user.username || '',
      password: '',
      fullName: user.fullName || '',
      email: user.email || '',
      phone: user.phone || '',
      role: user.role || 'CUSTOMER',
      status: user.status || 'ACTIVE'
    });
    setUserDialogOpen(true);
  };

  const handleSaveUser = async () => {
    try {
      if (selectedUser) {
        // Cập nhật người dùng hiện có
        await updateUser(selectedUser.id, userForm);
        toast.success('Cập nhật người dùng thành công');
      } else {
        // Tạo người dùng mới
        const username = userForm.username.trim() || userForm.email.split('@')[0] || `user_${Date.now()}`;
        const password = userForm.password.trim() || '123456';
        
        await register({
          username,
          password,
          fullName: userForm.fullName,
          email: userForm.email,
          phone: userForm.phone,
          role: userForm.role as any
        });
        toast.success(`Thêm người dùng thành công. Mật khẩu: ${password}`);
      }
      setUserDialogOpen(false);
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể lưu người dùng: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  const handleDeleteUser = async (userId: number, username: string) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa người dùng "${username}"?`)) {
      return;
    }
    try {
      await deleteUser(userId);
      toast.success('Xóa người dùng thành công');
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể xóa người dùng: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  // Order management handlers
  const handleUpdateOrderStatus = async (orderId: number, newStatus: string) => {
    try {
      await updateOrderStatus(orderId, newStatus);
      toast.success('Cập nhật trạng thái đơn hàng thành công');
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể cập nhật trạng thái: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  // Menu management handlers
  const handleEditMenuItem = (item: MenuItem) => {
    setSelectedMenuItem(item);
    setMenuForm({
      name: item.name || '',
      description: item.description || '',
      price: item.price?.toString() || '',
      categoryId: item.categoryId?.toString() || '',
      imageUrl: item.imageUrl || '',
      isAvailable: item.isAvailable ?? true
    });
    setMenuDialogOpen(true);
  };

  const handleAddMenuItem = () => {
    setSelectedMenuItem(null);
    setMenuForm({ name: '', description: '', price: '', categoryId: '', imageUrl: '', isAvailable: true });
    setMenuDialogOpen(true);
  };

  const handleSaveMenuItem = async () => {
    try {
      if (selectedMenuItem) {
        await updateMenuItem(selectedMenuItem.id, {
          ...menuForm,
          price: parseFloat(menuForm.price) || 0,
          categoryId: parseInt(menuForm.categoryId) || undefined
        });
        toast.success('Cập nhật món ăn thành công');
      } else {
        await createMenuItem({
          ...menuForm,
          price: parseFloat(menuForm.price) || 0,
          categoryId: parseInt(menuForm.categoryId) || undefined
        } as any);
        toast.success('Thêm món ăn thành công');
      }
      setMenuDialogOpen(false);
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể lưu món ăn: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  const handleDeleteMenuItem = async (itemId: number, itemName: string) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa món "${itemName}"?`)) {
      return;
    }
    try {
      await deleteMenuItem(itemId);
      toast.success('Xóa món ăn thành công');
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể xóa món ăn: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  // Table status handler
  const handleUpdateTableStatus = async (tableId: number, newStatus: string) => {
    try {
      await updateTableStatus(tableId, newStatus);
      toast.success('Cập nhật trạng thái bàn thành công');
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể cập nhật trạng thái: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  const handleSendQRCode = async (tableId: number, tableName: string) => {
    try {
      const result = await sendQRCodeToESP32(tableId);
      toast.success(`Đã gửi QR code của bàn ${tableName} tới ESP32 thành công!`);
    } catch (error: any) {
      toast.error('Không thể gửi QR code: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  // Category handlers
  const handleAddCategory = () => {
    setSelectedCategory(null);
    setCategoryForm({ name: '', description: '' });
    setCategoryDialogOpen(true);
  };

  const handleEditCategory = (cat: Category) => {
    setSelectedCategory(cat);
    setCategoryForm({ name: cat.name || '', description: cat.description || '' });
    setCategoryDialogOpen(true);
  };

  const handleSaveCategory = async () => {
    try {
      if (selectedCategory) {
        await updateCategory(selectedCategory.id, categoryForm);
        toast.success('Cập nhật danh mục thành công');
      } else {
        await createCategory(categoryForm);
        toast.success('Thêm danh mục thành công');
      }
      setCategoryDialogOpen(false);
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể lưu danh mục: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  const handleDeleteCategory = async (catId: number, name: string) => {
    if (!confirm(`Bạn có chắc muốn xóa danh mục "${name}"? Các món ăn trong danh mục này có thể bị ảnh hưởng.`)) {
      return;
    }
    try {
      await deleteCategory(catId);
      toast.success('Xóa danh mục thành công');
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể xóa danh mục: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  // Table CRUD handlers
  const handleAddTable = () => {
    setSelectedTableForEdit(null);
    setTableForm({ tableName: '', capacity: '', status: 'AVAILABLE', tableType: 'INDOOR', location: '' });
    setTableDialogOpen(true);
  };

  const handleEditTable = (table: RestaurantTable) => {
    setSelectedTableForEdit(table);
    setTableForm({
      tableName: table.tableName || '',
      capacity: table.capacity?.toString() || '0',
      status: table.status || 'AVAILABLE',
      tableType: table.tableType || 'INDOOR',
      location: table.location || ''
    });
    setTableDialogOpen(true);
  };

  const handleSaveTable = async () => {
    try {
      const payload = {
        tableName: tableForm.tableName,
        capacity: parseInt(tableForm.capacity) || 0,
        status: tableForm.status,
        tableType: tableForm.tableType,
        location: tableForm.location
      };

      if (selectedTableForEdit) {
        await updateTable(selectedTableForEdit.id, payload);
        toast.success('Cập nhật bàn thành công');
      } else {
        await createTable(payload);
        toast.success('Thêm bàn thành công');
      }
      setTableDialogOpen(false);
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể lưu bàn ăn: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  const handleDeleteTable = async (tableId: number, tableName: string) => {
    if (!confirm(`Bạn có chắc muốn xóa bàn "${tableName}"?`)) {
      return;
    }
    try {
      await deleteTable(tableId);
      toast.success('Xóa bàn thành công');
      await loadDashboardData();
    } catch (error: any) {
      toast.error('Không thể xóa bàn: ' + (error.message || 'Lỗi không xác định'));
    }
  };

  // Calculate statistics from dashboard summary
  const stats = [
    {
      title: "Tổng doanh thu",
      value: dashboardSummary 
        ? `${(dashboardSummary.totalRevenue ?? 0).toLocaleString('vi-VN')}đ`
        : '0đ',
      icon: DollarSign,
      color: "text-green-600",
      bgColor: "bg-green-50 dark:bg-green-900/20"
    },
    {
      title: "Đơn hàng hoạt động",
      value: dashboardSummary?.activeOrders.toString() || '0',
      icon: ShoppingCart,
      color: "text-blue-600",
      bgColor: "bg-blue-50 dark:bg-blue-900/20"
    },
    {
      title: "Bàn trống",
      value: dashboardSummary
        ? `${dashboardSummary.availableTables}/${dashboardSummary.totalTables}`
        : '0/0',
      icon: Table,
      color: "text-emerald-600",
      bgColor: "bg-emerald-50 dark:bg-emerald-900/20"
    },
    {
      title: "Đặt bàn chờ duyệt",
      value: dashboardSummary?.pendingReservations.toString() || '0',
      icon: Calendar,
      color: "text-orange-600",
      bgColor: "bg-orange-50 dark:bg-orange-900/20"
    }
  ];

  return (
    console.log('totalItems:', totalItems),
    
    <div className="min-h-screen bg-gradient-to-b from-background to-muted">
      {/* Header */}
      <header className="border-b bg-card/80 backdrop-blur-sm">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-gradient-to-br from-purple-500 to-pink-500 rounded-lg flex items-center justify-center text-primary-foreground font-bold">
              👑
            
              
            </div>
            <div>
              <h1 className="text-xl font-bold">Admin Dashboard</h1>
              <p className="text-sm text-muted-foreground">Chào mừng Admin {user?.username}</p>
              
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Badge variant="secondary" className="bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200">
              ADMIN
            </Badge>
            <Button variant="outline" onClick={handleLogout} className="flex items-center gap-2">
              <LogOut className="w-4 h-4" />
              Đăng xuất
            </Button>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
          <TabsList className="grid w-full grid-cols-5 mb-8">
            <TabsTrigger value="overview">Tổng quan</TabsTrigger>
            <TabsTrigger value="users">Người dùng</TabsTrigger>
            <TabsTrigger value="orders">Đơn hàng</TabsTrigger>
            <TabsTrigger value="menu">Thực đơn</TabsTrigger>
            <TabsTrigger value="tables">Bàn ăn</TabsTrigger>
          </TabsList>

          {/* Overview Tab */}
          <TabsContent value="overview" className="space-y-6">
            {/* Stats Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {stats.map((stat, idx) => (
                <Card key={idx}>
                  <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                    <CardTitle className="text-sm font-medium">{stat.title}</CardTitle>
                    <div className={`p-2 rounded-lg ${stat.bgColor}`}>
                      <stat.icon className={`h-4 w-4 ${stat.color}`} />
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="text-2xl font-bold">{stat.value}</div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* Recent Activity */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle>Đơn hàng gần đây</CardTitle>
                </CardHeader>
                <CardContent>
                  {loadingDashboard ? (
                    <div className="flex items-center justify-center py-8">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                    </div>
                  ) : recentOrders.length === 0 ? (
                    <p className="text-muted-foreground text-center py-8">Chưa có đơn hàng</p>
                  ) : (
                    <div className="space-y-3">
                      {recentOrders.map((order) => {
                        const paymentStatusText = 
                          order.paymentStatus === 'WAITING_PAYMENT' ? 'Đang chờ thanh toán' :
                          order.paymentStatus === 'PAID' ? 'Đã thanh toán' :
                          order.paymentStatus === 'CANCELLED' ? 'Đã hủy' :
                          order.paymentStatus || 'Đang chờ thanh toán';
                        
                        return (
                          <div key={order.orderId} className="flex items-center justify-between p-3 bg-muted/50 rounded-lg">
                            <div>
                              <p className="font-medium">Đơn #{order.orderId}</p>
                              <p className="text-sm text-muted-foreground">
                                {new Date(order.createdAt).toLocaleDateString('vi-VN', {
                                  day: '2-digit',
                                  month: '2-digit',
                                  year: 'numeric',
                                  hour: '2-digit',
                                  minute: '2-digit'
                                })}
                              </p>
                            </div>
                            <div className="text-right">
                              <p className="font-medium">{(order.totalAmount ?? 0).toLocaleString('vi-VN')}đ</p>
                              <Badge variant={
                                order.paymentStatus === 'PAID' ? 'default' :
                                order.paymentStatus === 'CANCELLED' ? 'destructive' : 'secondary'
                              }>
                                {paymentStatusText}
                              </Badge>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Đặt bàn chờ duyệt</CardTitle>
                </CardHeader>
                <CardContent>
                  {loadingDashboard ? (
                    <div className="flex items-center justify-center py-8">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                    </div>
                  ) : pendingReservations.length === 0 ? (
                    <p className="text-muted-foreground text-center py-8">Không có đặt bàn nào chờ duyệt</p>
                  ) : (
                    <div className="space-y-3">
                      {pendingReservations.map((booking) => (
                        <div key={booking.id} className="p-3 bg-muted/50 rounded-lg space-y-2">
                          <div className="flex items-start justify-between">
                            <div className="flex-1">
                              <p className="font-medium">{booking.customer?.username || booking.customer?.fullName || 'Khách'}</p>
                              {booking.customer?.fullName && booking.customer?.username && (
                                <p className="text-xs text-muted-foreground">@{booking.customer.username}</p>
                              )}
                              <div className="mt-1 space-y-0.5">
                                {booking.customer?.email && (
                                  <p className="text-xs text-muted-foreground">📧 {booking.customer.email}</p>
                                )}
                                {booking.customer?.phone && (
                                  <p className="text-xs text-muted-foreground">📞 {booking.customer.phone}</p>
                                )}
                              </div>
                            </div>
                            <div className="text-right ml-4">
                              <p className="text-sm font-medium">{booking.guests} người</p>
                              <p className="text-xs text-muted-foreground mt-1">
                                {new Date(booking.date).toLocaleDateString('vi-VN')} - {booking.time}
                              </p>
                            </div>
                          </div>
                          <div className="flex gap-2 pt-2 border-t">
                            <Button 
                              size="sm" 
                              variant="default" 
                              className="flex-1"
                              onClick={() => handleApproveReservation(booking.id)}
                            >
                              <CheckCircle className="w-4 h-4 mr-1" />
                              Duyệt
                            </Button>
                            <Button 
                              size="sm" 
                              variant="destructive" 
                              className="flex-1"
                              onClick={() => handleRejectReservation(booking.id)}
                            >
                              <XCircle className="w-4 h-4 mr-1" />
                              Từ chối
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* Users Tab */}
          <TabsContent value="users" className="space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-2xl font-bold">Quản lý người dùng</h2>
              <Button onClick={handleAddUser}>
                <Plus className="w-4 h-4 mr-2" />
                Thêm người dùng
              </Button>
            </div>

            <Card>
              <CardContent className="p-0">
                {loading ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                  </div>
                ) : users.length === 0 ? (
                  <p className="text-muted-foreground text-center py-8">Chưa có người dùng</p>
                ) : (
                  <div className="divide-y">
                    {users.map((userItem) => (
                      <div key={userItem.id} className="flex items-center justify-between p-4">
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                            <Users className="w-5 h-5" />
                          </div>
                          <div>
                            <p className="font-medium">{userItem.username}</p>
                            <p className="text-sm text-muted-foreground">{userItem.email}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <Badge variant={
                            userItem.role === 'ADMIN' ? 'default' :
                            userItem.role === 'STAFF' ? 'secondary' :
                            userItem.role === 'KITCHEN' ? 'outline' : 'outline'
                          }>
                            {userItem.role}
                          </Badge>
                          <Button variant="outline" size="sm" onClick={() => handleEditUser(userItem)}>
                            <Edit className="w-4 h-4 mr-1" />
                            Sửa
                          </Button>
                          <Button 
                            variant="destructive" 
                            size="sm" 
                            onClick={() => handleDeleteUser(userItem.id, userItem.username)}
                          >
                            <Trash2 className="w-4 h-4 mr-1" />
                            Xóa
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          {/* Orders Tab */}
          <TabsContent value="orders" className="space-y-6">
            <h2 className="text-2xl font-bold">Quản lý đơn hàng</h2>
            <Card>
              <CardContent className="p-0">
                {loading ? (
                  <div className="flex items-center justify-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
                  </div>
                ) : orders.length === 0 ? (
                  <p className="text-muted-foreground text-center py-8">Chưa có đơn hàng</p>
                ) : (
                  <div className="divide-y">
                    {orders.map((order) => (
                      <div key={order.id} className="p-4">
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                              <p className="font-medium">Đơn #{order.id}</p>
                              <Badge variant={
                                order.status === 'PLACED' || order.status === 'CONFIRMED' ? 'default' :
                                order.status === 'CANCELLED' ? 'destructive' : 'secondary'
                              }>
                                {order.status}
                              </Badge>
                              {order.paymentStatus && (
                                <Badge variant={
                                  order.paymentStatus === 'PAID' ? 'default' :
                                  order.paymentStatus === 'CANCELLED' ? 'destructive' : 'secondary'
                                }>
                                  {order.paymentStatus === 'PAID' ? 'Đã thanh toán' :
                                   order.paymentStatus === 'WAITING_PAYMENT' ? 'Chờ thanh toán' : order.paymentStatus}
                                </Badge>
                              )}
                            </div>
                            <div className="space-y-1 text-sm text-muted-foreground">
                              <p>Bàn: {order.table?.tableName || `#${order.tableId}`}</p>
                              {order.customer && (
                                <p>Khách: {order.customer.fullName || order.customer.username}</p>
                              )}
                              <p>Tổng: {(order.totalAmount ?? 0).toLocaleString('vi-VN')}đ</p>
                              <p>Ngày: {order.createdAt ? new Date(order.createdAt).toLocaleString('vi-VN') : 'Chưa rõ'}</p>
                            </div>
                          </div>
                          <Select
                            value={order.status}
                            onValueChange={(value) => handleUpdateOrderStatus(order.id, value)}
                          >
                            <SelectTrigger className="w-[180px]">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="PLACED">PLACED</SelectItem>
                              <SelectItem value="CONFIRMED">CONFIRMED</SelectItem>
                              <SelectItem value="PREPARING">PREPARING</SelectItem>
                              <SelectItem value="READY">READY</SelectItem>
                              <SelectItem value="SERVED">SERVED</SelectItem>
                              <SelectItem value="COMPLETED">COMPLETED</SelectItem>
                              <SelectItem value="CANCELLED">CANCELLED</SelectItem>
                            </SelectContent>
                          </Select>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          {/* Menu Tab */}
          <TabsContent value="menu" className="space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-2xl font-bold">Quản lý thực đơn</h2>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={async () => {
                    try {
                      await fetch('/api/categories/cleanup-duplicates', { method: 'POST' });
                      toast.success('Cleanup initiated. Please run the SQL script.');
                      loadDashboardData();
                    } catch (error) {
                      toast.error('Cleanup failed');
                    }
                  }}
                >
                  Cleanup Categories
                </Button>
                <Button onClick={handleAddCategory} className="bg-green-600 hover:bg-green-700 text-white">
                  <Plus className="w-4 h-4 mr-2" />
                  Thêm danh mục
                </Button>
                <Button onClick={handleAddMenuItem}>
                  <Plus className="w-4 h-4 mr-2" />
                  Thêm món ăn
                </Button>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <Card>
                <CardHeader>
                  <CardTitle>Danh mục ({categories.length})</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2 max-h-[600px] overflow-y-auto pr-2">
                    {categories.map((category) => (
                      <div key={category.id} className="flex items-center justify-between py-2 border-b last:border-b-0">
                        <div>
                          <p className="font-semibold">{category.name}</p>
                          {category.description && (
                            <p className="text-xs text-muted-foreground line-clamp-1">{category.description}</p>
                          )}
                        </div>
                        <div className="flex gap-2">
                          <Button variant="outline" size="sm" onClick={() => handleEditCategory(category)}>
                            <Edit className="w-4 h-4 mr-1" />
                            Sửa
                          </Button>
                          <Button variant="destructive" size="sm" onClick={() => handleDeleteCategory(category.id, category.name)}>
                            <Trash2 className="w-4 h-4 mr-1" />
                            Xóa
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardHeader>
                  <CardTitle>Món ăn ({menuItems.length})</CardTitle>
                </CardHeader>
                <CardContent className="p-0">
                  {menuItems.length === 0 ? (
                    <p className="text-muted-foreground text-center py-8">Chưa có món ăn</p>
                  ) : (
                    <div className="divide-y max-h-[600px] overflow-y-auto">
                      {menuItems.map((item) => (
                        <div key={item.id} className="flex items-center justify-between p-4 hover:bg-muted/50">
                          <div className="flex-1">
                            <p className="font-medium">{item.name}</p>
                            <p className="text-sm text-muted-foreground">
                              {item.price?.toLocaleString('vi-VN')}đ • {item.category?.name || 'Chưa phân loại'}
                            </p>
                            {item.description && (
                              <p className="text-xs text-muted-foreground mt-1 line-clamp-1">{item.description}</p>
                            )}
                          </div>
                          <div className="flex items-center gap-2">
                            <Badge variant={item.isAvailable ? 'default' : 'secondary'}>
                              {item.isAvailable ? 'Có sẵn' : 'Hết'}
                            </Badge>
                            <Button 
                              variant="outline" 
                              size="sm" 
                              onClick={() => handleEditMenuItem(item)}
                            >
                              <Edit className="w-4 h-4 mr-1" />
                              Sửa
                            </Button>
                            <Button 
                              variant="destructive" 
                              size="sm" 
                              onClick={() => handleDeleteMenuItem(item.id, item.name)}
                            >
                              <Trash2 className="w-4 h-4 mr-1" />
                              Xóa
                            </Button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          {/* Tables Tab */}
          <TabsContent value="tables" className="space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-2xl font-bold">Quản lý bàn ăn</h2>
              <Button onClick={handleAddTable}>
                <Plus className="w-4 h-4 mr-2" />
                Thêm bàn
              </Button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {tables.map((table) => (
                <Card key={table.id} className={`cursor-pointer transition-all hover:shadow-lg ${
                  table.status === 'AVAILABLE' ? 'border-green-200 bg-green-50 dark:bg-green-900/20' :
                  table.status === 'OCCUPIED' ? 'border-red-200 bg-red-50 dark:bg-red-900/20' :
                  'border-gray-200'
                }`}>
                  <CardContent className="p-4 text-center">
                    <div className="w-12 h-12 mx-auto mb-2 bg-muted rounded-full flex items-center justify-center">
                      <Table className="w-6 h-6" />
                    </div>
                    <p className="font-semibold">{table.tableName}</p>
                    <p className="text-sm text-muted-foreground">{table.capacity} người • {table.tableType || 'INDOOR'}</p>
                    <div className="mt-3 space-y-2">
                      <Select
                        value={table.status || 'AVAILABLE'}
                        onValueChange={(value) => handleUpdateTableStatus(table.id, value)}
                      >
                        <SelectTrigger className="w-full">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="AVAILABLE">Trống</SelectItem>
                          <SelectItem value="OCCUPIED">Đang dùng</SelectItem>
                          <SelectItem value="RESERVED">Đã đặt</SelectItem>
                          <SelectItem value="CLEANING">Đang dọn</SelectItem>
                          <SelectItem value="MAINTENANCE">Bảo trì</SelectItem>
                        </SelectContent>
                      </Select>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          className="flex-1"
                          onClick={() => handleEditTable(table)}
                        >
                          <Edit className="w-4 h-4 mr-1" />
                          Sửa
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          className="flex-1"
                          onClick={() => handleDeleteTable(table.id, table.tableName)}
                        >
                          <Trash2 className="w-4 h-4 mr-1" />
                          Xóa
                        </Button>
                      </div>
                      <Button
                        size="sm"
                        variant="outline"
                        className="w-full"
                        onClick={() => handleSendQRCode(table.id, table.tableName)}
                      >
                        <QrCode className="w-4 h-4 mr-2" />
                        Gửi QR Code
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </TabsContent>
        </Tabs>
      </div>

      {/* User Edit Dialog */}
      <Dialog open={userDialogOpen} onOpenChange={setUserDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{selectedUser ? 'Cập nhật người dùng' : 'Thêm người dùng mới'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {!selectedUser && (
              <>
                <div className="space-y-2">
                  <Label htmlFor="username">Tên đăng nhập (Username) *</Label>
                  <Input
                    id="username"
                    value={userForm.username}
                    onChange={(e) => setUserForm({ ...userForm, username: e.target.value })}
                    placeholder="Nhập tên đăng nhập"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="password">Mật khẩu *</Label>
                  <Input
                    id="password"
                    type="password"
                    value={userForm.password}
                    onChange={(e) => setUserForm({ ...userForm, password: e.target.value })}
                    placeholder="Nhập mật khẩu (Mặc định: 123456 nếu bỏ trống)"
                  />
                </div>
              </>
            )}
            <div className="space-y-2">
              <Label htmlFor="fullName">Họ tên</Label>
              <Input
                id="fullName"
                value={userForm.fullName}
                onChange={(e) => setUserForm({ ...userForm, fullName: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={userForm.email}
                onChange={(e) => setUserForm({ ...userForm, email: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="phone">Số điện thoại</Label>
              <Input
                id="phone"
                value={userForm.phone}
                onChange={(e) => setUserForm({ ...userForm, phone: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="role">Vai trò</Label>
              <Select value={userForm.role} onValueChange={(value) => setUserForm({ ...userForm, role: value })}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ADMIN">ADMIN</SelectItem>
                  <SelectItem value="STAFF">STAFF</SelectItem>
                  <SelectItem value="KITCHEN">KITCHEN</SelectItem>
                  <SelectItem value="CUSTOMER">CUSTOMER</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="status">Trạng thái</Label>
              <Select value={userForm.status} onValueChange={(value) => setUserForm({ ...userForm, status: value })}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">ACTIVE</SelectItem>
                  <SelectItem value="INACTIVE">INACTIVE</SelectItem>
                  <SelectItem value="SUSPENDED">SUSPENDED</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setUserDialogOpen(false)}>Hủy</Button>
            <Button onClick={handleSaveUser}>
              {selectedUser ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Menu Item Dialog */}
      <Dialog open={menuDialogOpen} onOpenChange={setMenuDialogOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{selectedMenuItem ? 'Cập nhật món ăn' : 'Thêm món ăn mới'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4 max-h-[60vh] overflow-y-auto">
            <div className="space-y-2">
              <Label htmlFor="menuName">Tên món</Label>
              <Input
                id="menuName"
                value={menuForm.name}
                onChange={(e) => setMenuForm({ ...menuForm, name: e.target.value })}
                placeholder="Nhập tên món ăn"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="menuDescription">Mô tả</Label>
              <Textarea
                id="menuDescription"
                value={menuForm.description}
                onChange={(e) => setMenuForm({ ...menuForm, description: e.target.value })}
                placeholder="Nhập mô tả món ăn"
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="menuPrice">Giá (VNĐ)</Label>
                <Input
                  id="menuPrice"
                  type="number"
                  value={menuForm.price}
                  onChange={(e) => setMenuForm({ ...menuForm, price: e.target.value })}
                  placeholder="0"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="menuCategory">Danh mục</Label>
                <Select value={menuForm.categoryId} onValueChange={(value) => setMenuForm({ ...menuForm, categoryId: value })}>
                  <SelectTrigger>
                    <SelectValue placeholder="Chọn danh mục" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={cat.id.toString()}>{cat.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="menuImageUrl">URL hình ảnh</Label>
              <Input
                id="menuImageUrl"
                value={menuForm.imageUrl}
                onChange={(e) => setMenuForm({ ...menuForm, imageUrl: e.target.value })}
                placeholder="https://..."
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="menuAvailable">Trạng thái</Label>
              <Select
                value={menuForm.isAvailable ? 'true' : 'false'}
                onValueChange={(value) => setMenuForm({ ...menuForm, isAvailable: value === 'true' })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="true">Có sẵn</SelectItem>
                  <SelectItem value="false">Hết</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setMenuDialogOpen(false)}>Hủy</Button>
            <Button onClick={handleSaveMenuItem}>
              {selectedMenuItem ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Category CRUD Dialog */}
      <Dialog open={categoryDialogOpen} onOpenChange={setCategoryDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{selectedCategory ? 'Cập nhật danh mục' : 'Thêm danh mục mới'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="categoryName">Tên danh mục</Label>
              <Input
                id="categoryName"
                value={categoryForm.name}
                onChange={(e) => setCategoryForm({ ...categoryForm, name: e.target.value })}
                placeholder="Nhập tên danh mục"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="categoryDescription">Mô tả</Label>
              <Textarea
                id="categoryDescription"
                value={categoryForm.description}
                onChange={(e) => setCategoryForm({ ...categoryForm, description: e.target.value })}
                placeholder="Nhập mô tả cho danh mục"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCategoryDialogOpen(false)}>Hủy</Button>
            <Button onClick={handleSaveCategory}>
              {selectedCategory ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Table CRUD Dialog */}
      <Dialog open={tableDialogOpen} onOpenChange={setTableDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{selectedTableForEdit ? 'Cập nhật bàn ăn' : 'Thêm bàn ăn mới'}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="tableName">Tên bàn</Label>
              <Input
                id="tableName"
                value={tableForm.tableName}
                onChange={(e) => setTableForm({ ...tableForm, tableName: e.target.value })}
                placeholder="Ví dụ: Bàn 15"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="tableCapacity">Sức chứa (người)</Label>
                <Input
                  id="tableCapacity"
                  type="number"
                  value={tableForm.capacity}
                  onChange={(e) => setTableForm({ ...tableForm, capacity: e.target.value })}
                  placeholder="4"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="tableType">Loại bàn</Label>
                <Select value={tableForm.tableType} onValueChange={(value) => setTableForm({ ...tableForm, tableType: value })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="INDOOR">Trong nhà (INDOOR)</SelectItem>
                    <SelectItem value="OUTDOOR">Sân vườn (OUTDOOR)</SelectItem>
                    <SelectItem value="WINDOW">Cạnh cửa sổ (WINDOW)</SelectItem>
                    <SelectItem value="VIP">Phòng VIP (VIP)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="tableStatus">Trạng thái</Label>
                <Select value={tableForm.status} onValueChange={(value) => setTableForm({ ...tableForm, status: value })}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="AVAILABLE">Trống (AVAILABLE)</SelectItem>
                    <SelectItem value="OCCUPIED">Đang dùng (OCCUPIED)</SelectItem>
                    <SelectItem value="RESERVED">Đã đặt (RESERVED)</SelectItem>
                    <SelectItem value="CLEANING">Đang dọn (CLEANING)</SelectItem>
                    <SelectItem value="MAINTENANCE">Bảo trì (MAINTENANCE)</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="tableLocation">Khu vực / Vị trí</Label>
                <Input
                  id="tableLocation"
                  value={tableForm.location}
                  onChange={(e) => setTableForm({ ...tableForm, location: e.target.value })}
                  placeholder="Ví dụ: Khu A"
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTableDialogOpen(false)}>Hủy</Button>
            <Button onClick={handleSaveTable}>
              {selectedTableForEdit ? 'Cập nhật' : 'Thêm mới'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export default function AdminDashboard() {
  return (
    <AdminOnly>
      <AdminDashboardContent />
    </AdminOnly>
  );
}
