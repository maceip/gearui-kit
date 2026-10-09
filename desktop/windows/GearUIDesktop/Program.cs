using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace GearUIDesktop;

internal static class Program
{
    internal static readonly string LogPath =
        Path.Combine(Path.GetTempPath(), "gearui-desktop.log");

    internal static void Log(string line)
    {
        try
        {
            File.AppendAllText(LogPath, DateTime.Now.ToString("HH:mm:ss.fff") + " " + line + Environment.NewLine);
        }
        catch
        {
            // A missing log must not take the window down.
        }
    }

    [STAThread]
    private static void Main(string[] args)
    {
        Application.SetUnhandledExceptionMode(UnhandledExceptionMode.CatchException);
        Application.ThreadException += (_, e) => Log("ui " + e.Exception);
        AppDomain.CurrentDomain.UnhandledException += (_, e) => Log("crash " + e.ExceptionObject);

        var content = args.Length > 0
            ? args[0]
            : Path.Combine(AppContext.BaseDirectory, "www");
        Log("start " + content);
        if (!File.Exists(Path.Combine(content, "index.html")))
        {
            Log("missing index.html");
            MessageBox.Show(
                "GearUI desktop content was not found at:\n" + content,
                "GearUI",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
            return;
        }

        ApplicationConfiguration.Initialize();
        Application.Run(new DesktopWindow(content));
        Log("exit");
    }
}

/// <summary>
/// A normal Windows window. The title bar, resize borders, and caption buttons
/// belong to the system. GearUI fills the client area.
/// </summary>
internal sealed class DesktopWindow : Form
{
    private readonly WebView2 _web = new() { Dock = DockStyle.Fill };

    internal DesktopWindow(string contentDirectory)
    {
        Text = "GearUI";
        StartPosition = FormStartPosition.CenterScreen;
        WindowState = FormWindowState.Maximized;
        MinimumSize = new Size(800, 600);
        Controls.Add(_web);
        FormClosed += (_, e) => Program.Log("closed " + e.CloseReason);
        Load += async (_, _) =>
        {
            try
            {
                await Open(contentDirectory);
            }
            catch (Exception ex)
            {
                Program.Log("open failed " + ex);
                MessageBox.Show(ex.Message, "GearUI", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        };
    }

    private async Task Open(string contentDirectory)
    {
        // A fresh profile so a rebuilt page is not served from the last run's cache.
        var profile = Path.Combine(Path.GetTempPath(), "gearui-webview");
        var environment = await CoreWebView2Environment.CreateAsync(null, profile);
        await _web.EnsureCoreWebView2Async(environment);
        var core = _web.CoreWebView2;
        core.ProcessFailed += (_, e) => Program.Log("webview " + e.ProcessFailedKind);
        core.Settings.AreDefaultContextMenusEnabled = false;
        core.Settings.AreDevToolsEnabled = false;
        core.Settings.IsStatusBarEnabled = false;
        core.SetVirtualHostNameToFolderMapping(
            "gearui.desktop",
            contentDirectory,
            CoreWebView2HostResourceAccessKind.Allow);
        core.Navigate(
            "https://gearui.desktop/index.html?page_name=DesktopHost&systemTitleBar=1&lang=zh-Hans");
        Program.Log("navigated");
    }
}
