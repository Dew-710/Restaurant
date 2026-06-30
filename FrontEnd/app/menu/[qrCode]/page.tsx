'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { getTableByQr, getMenuItems, getCategories, createOrderFromRequest, addItemsToOrder, getActiveBookingByTable, getActiveOrdersByTable } from '@/lib/api';
import { useAuth } from '@/lib/context/auth-context';
import type { RestaurantTable, MenuItem, Category, Booking, Order } from '@/lib/types';
import { ShoppingCart, Loader2, Utensils } from 'lucide-react';
import { toast } from 'sonner';

export default function CustomerMenuPage() {
  const params = useParams();
  const qrCode = params?.qrCode as string;
  const { user } = useAuth();
  
  const [table, setTable] = useState<RestaurantTable | null>(null);
  const [booking, setBooking] = useState<Booking | null>(null);
  const [activeOrder, setActiveOrder] = useState<Order | null>(null);
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [cart, setCart] = useState<{ menuItem: MenuItem; quantity: number }[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (qrCode) {
      loadData();
    }
  }, [qrCode]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [tableRes, menuRes, categoriesRes] = await Promise.all([
        getTableByQr(qrCode),
        getMenuItems(),
        getCategories(),
      ]);

      setTable(tableRes.table);
      setMenuItems(menuRes.menuItems || []);
      setCategories(categoriesRes.categories || []);

      // Load active booking and active order for this table
      if (tableRes.table?.id) {
        try {
          const bookingRes = await getActiveBookingByTable(tableRes.table.id);
          if (bookingRes.hasActiveBooking && bookingRes.booking) {
            setBooking(bookingRes.booking);
          }
        } catch (error) {
          // No booking found, that's okay
          setBooking(null);
        }

        // Check for active orders
        try {
          const ordersRes = await getActiveOrdersByTable(tableRes.table.id);
          if (ordersRes.orders && ordersRes.orders.length > 0) {
            setActiveOrder(ordersRes.orders[0]); // Use first active order
          }
        } catch (error) {
          // No active order, that's okay
          setActiveOrder(null);
        }
      }
    } catch (error) {
      toast.error('Không thể tải dữ liệu');
    } finally {
      setLoading(false);
    }
  };

  const handleAddToCart = (menuItem: MenuItem) => {
    const existingItem = cart.find(item => item.menuItem.id === menuItem.id);
    if (existingItem) {
      setCart(cart.map(item =>
        item.menuItem.id === menuItem.id
          ? { ...item, quantity: item.quantity + 1 }
          : item
      ));
    } else {
      setCart([...cart, { menuItem, quantity: 1 }]);
    }
    toast.success(`Đã thêm ${menuItem.name} vào giỏ hàng`);
  };

  const handleUpdateQuantity = (menuItemId: number, quantity: number) => {
    if (quantity <= 0) {
      setCart(cart.filter(item => item.menuItem.id !== menuItemId));
    } else {
      setCart(cart.map(item =>
        item.menuItem.id === menuItemId
          ? { ...item, quantity }
          : item
      ));
    }
  };

  const handleSubmitOrder = async () => {
    if (!table || cart.length === 0) {
      toast.error('Vui lòng chọn món ăn');
      return;
    }

    try {
      setSubmitting(true);
      
      let orderId: number;

      // Check if there's already an active order for this table
      if (activeOrder && activeOrder.id) {
        // Use existing order
        orderId = activeOrder.id;
        console.log('Using existing active order:', orderId);
      } else {
        // Create new order
        // Customer ID: từ booking nếu có, hoặc undefined (sẽ không gửi field này)
        const customerId = booking?.customerId;
        const bookingId = booking?.id;

        const orderRequest: any = {
          tableId: table.id,
          status: 'PLACED',
          items: []
        };

        // Only add customerId if it exists
        if (customerId !== undefined && customerId !== null) {
          orderRequest.customerId = customerId;
        }

        // Only add bookingId if it exists
        if (bookingId !== undefined && bookingId !== null) {
          orderRequest.bookingId = bookingId;
        }

        console.log('Creating new order with request:', JSON.stringify(orderRequest, null, 2));
        console.log('Table:', table);
        console.log('Booking:', booking);
        
        const orderResult = await createOrderFromRequest(orderRequest);
        orderId = orderResult.order.id;
        setActiveOrder(orderResult.order);
      }

      // Add items to order - need to send full menuItem objects
      const orderItems = cart.map(item => ({
        menuItem: {
          id: item.menuItem.id,
          name: item.menuItem.name,
          price: item.menuItem.price,
          description: item.menuItem.description,
          imageUrl: item.menuItem.imageUrl,
          category: item.menuItem.category,
          isAvailable: item.menuItem.isAvailable,
          preparationTime: item.menuItem.preparationTime,
          calories: item.menuItem.calories,
          allergens: item.menuItem.allergens
        },
        quantity: item.quantity,
        price: item.menuItem.price
      }));

      console.log('Adding items to order:', orderId, orderItems);
      await addItemsToOrder(orderId, orderItems as any);
      
      toast.success('Đặt món thành công! Nhân viên sẽ phục vụ bạn sớm nhất có thể.');
      setCart([]);
    } catch (error: any) {
      console.error('Order submission error:', error);
      const errorMessage = error?.message || 'Đặt món thất bại. Vui lòng thử lại.';
      toast.error(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const getTotalPrice = () => {
    return cart.reduce((total, item) => total + (item.menuItem.price * item.quantity), 0);
  };

  const filteredMenuItems = selectedCategory
    ? menuItems.filter(item => item.category?.id === selectedCategory)
    : menuItems;

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-background to-muted flex items-center justify-center">
        <div className="text-center">
          <Loader2 className="w-8 h-8 animate-spin text-primary mx-auto mb-4" />
          <p className="text-muted-foreground">Đang tải menu...</p>
        </div>
      </div>
    );
  }

  if (!table) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-background to-muted flex items-center justify-center">
        <Card className="w-full max-w-md">
          <CardContent className="p-8 text-center">
            <Utensils className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
            <h1 className="text-2xl font-bold mb-2">Không tìm thấy bàn</h1>
            <p className="text-muted-foreground">
              Mã QR không hợp lệ hoặc bàn không tồn tại.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-background to-muted">
      {/* Header */}
      <header className="border-b bg-card/80 backdrop-blur-sm sticky top-0 z-10">
        <div className="container mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-xl font-bold">RestroFlow</h1>
              <p className="text-sm text-muted-foreground">Bàn {table.tableName}</p>
            </div>
            <Badge variant="secondary">
              {cart.length} món
            </Badge>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 py-8">
        {/* Categories Filter */}
        <div className="flex flex-wrap gap-2 mb-6">
          <Button
            variant={selectedCategory === null ? "default" : "outline"}
            size="sm"
            onClick={() => setSelectedCategory(null)}
          >
            Tất cả
          </Button>
          {categories.map((category) => (
            <Button
              key={category.id}
              variant={selectedCategory === category.id ? "default" : "outline"}
              size="sm"
              onClick={() => setSelectedCategory(category.id)}
            >
              {category.name}
            </Button>
          ))}
        </div>

        {/* Menu Items Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
          {filteredMenuItems
            .filter(item => item.isAvailable)
            .map((item) => (
              <Card key={item.id} className="hover:shadow-lg transition-shadow">
                <CardContent className="p-4">
                  <div className="text-center mb-3">
                    <div className="w-16 h-16 bg-muted rounded-full flex items-center justify-center mx-auto mb-2">
                      🍽️
                    </div>
                    <h3 className="font-semibold">{item.name}</h3>
                    <p className="text-sm text-muted-foreground line-clamp-2">{item.description}</p>
                  </div>
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-lg font-bold text-primary">
                      {item.price.toLocaleString('vi-VN')}đ
                    </span>
                    {item.category && (
                      <Badge variant="outline" className="text-xs">
                        {item.category.name}
                      </Badge>
                    )}
                  </div>
                  <Button
                    className="w-full"
                    size="sm"
                    onClick={() => handleAddToCart(item)}
                  >
                    Thêm vào giỏ
                  </Button>
                </CardContent>
              </Card>
            ))}
        </div>

        {/* Cart Sidebar */}
        {cart.length > 0 && (
          <div className="fixed bottom-0 left-0 right-0 bg-card border-t shadow-lg p-4">
            <div className="container mx-auto">
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <ShoppingCart className="w-5 h-5" />
                    <span className="font-semibold">Giỏ hàng ({cart.length} món)</span>
                  </div>
                  <div className="text-sm text-muted-foreground">
                    {cart.map((item, idx) => (
                      <div key={idx} className="flex items-center justify-between">
                        <span>{item.menuItem.name} x{item.quantity}</span>
                        <div className="flex items-center gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-6 w-6 p-0"
                            onClick={() => handleUpdateQuantity(item.menuItem.id, item.quantity - 1)}
                          >
                            -
                          </Button>
                          <span className="w-8 text-center">{item.quantity}</span>
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-6 w-6 p-0"
                            onClick={() => handleUpdateQuantity(item.menuItem.id, item.quantity + 1)}
                          >
                            +
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
                <div className="ml-6 text-right">
                  <p className="text-sm text-muted-foreground mb-1">Tổng cộng</p>
                  <p className="text-2xl font-bold text-primary mb-2">
                    {getTotalPrice().toLocaleString('vi-VN')}đ
                  </p>
                  <Button
                    onClick={handleSubmitOrder}
                    disabled={submitting}
                    className="w-full"
                  >
                    {submitting ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        Đang xử lý...
                      </>
                    ) : (
                      'Đặt món'
                    )}
                  </Button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

