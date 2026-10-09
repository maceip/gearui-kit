using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace Inventory;

public sealed partial class MainWindow : Window
{
    private bool sidebarOpen = true;

    public MainWindow()
    {
        InitializeComponent();
        ItemList.ItemsSource = InventoryBridge.Items;
    }

    private void ToggleSidebar(object sender, RoutedEventArgs e)
    {
        sidebarOpen = !sidebarOpen;
        SideColumn.Width = sidebarOpen ? new GridLength(220) : new GridLength(0);
        SidePanel.Visibility = sidebarOpen ? Visibility.Visible : Visibility.Collapsed;
        SidebarToggle.Content = sidebarOpen ? "Hide panel" : "Show panel";
    }

    private void ShowStock(object sender, RoutedEventArgs e) => ShowSection("Stock");

    private void ShowOrders(object sender, RoutedEventArgs e) => ShowSection("Orders");

    private void ShowNotes(object sender, RoutedEventArgs e) => ShowSection("Notes");

    private void ShowSection(string name)
    {
        ListHeading.Text = name;
        ItemList.SelectedItem = null;
        DetailName.Text = "Select an item";
        DetailId.Text = "";
        MetaBody.Text = "Nothing selected";
    }

    private void ItemChosen(object sender, SelectionChangedEventArgs e)
    {
        if (ItemList.SelectedItem is not InventoryLine item)
        {
            return;
        }

        DetailName.Text = item.Name;
        DetailId.Text = item.Id;
        MetaBody.Text = $"Quantity {item.Quantity}";
    }
}
