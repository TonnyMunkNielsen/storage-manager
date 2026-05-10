package net.tmn.storage_manager.web.vaadin;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;

public class MainLayout extends AppLayout {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(new DrawerToggle(), createTitle());
        addToDrawer(createNavigation());
    }

    private H1 createTitle() {
        H1 title = new H1("Produce Manager");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");
        return title;
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();
        nav.setLabel("Navigation");
        nav.addItem(new SideNavItem("Dashboard", DashboardView.class, VaadinIcon.DASHBOARD.create()));
        nav.addItem(new SideNavItem("Produce Types", ProduceTypesView.class, VaadinIcon.TAGS.create()));
        nav.addItem(new SideNavItem("Storage Boxes", StorageBoxesView.class, VaadinIcon.ARCHIVE.create()));
        nav.addItem(new SideNavItem("Produces", ProduceInstancesView.class, VaadinIcon.CUBES.create()));
        nav.addItem(new SideNavItem("Notifications", NotificationsView.class, VaadinIcon.BELL.create()));
        return nav;
    }
}
