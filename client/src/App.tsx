import { Switch, Route } from "wouter";
import { queryClient } from "./lib/queryClient";
import { QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import NotFound from "@/pages/not-found";
import AuthPage from "@/pages/auth-page";
import AdminDashboard from "@/pages/admin-dashboard";
import StaffDashboard from "@/pages/staff-dashboard";
import WidgetPage from "@/pages/widget-page";

function Router() {
  return (
    <Switch>
      <Route path="/" component={AuthPage} />
      <Route path="/admin" component={AdminDashboard} />
      <Route path="/staff" component={StaffDashboard} />
      <Route path="/widget" component={WidgetPage} />
      <Route path="/panel/" component={AuthPage} />
      <Route path="/panel/admin" component={AdminDashboard} />
      <Route path="/panel/staff" component={StaffDashboard} />
      <Route path="/panel/widget" component={WidgetPage} />
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <Toaster />
        <Router />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

export default App;
