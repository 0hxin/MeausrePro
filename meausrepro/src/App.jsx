import './App.css'
import {BrowserRouter, Route, Routes} from "react-router-dom";
import Main from "./page/Main.jsx";
import Login from "./page/Login.jsx";
import {UserProvider} from "./context/UserContext.jsx";
import UserManagement from "./page/UserManagement.jsx";
import GroupManagement from "./page/GroupManagement.jsx";
import InsPage from "./page/InsPage.jsx";
import CompAnalysis from "./component/chart/CompAnalysis.jsx";
import ProjectManagement from "./page/ProjectManagement.jsx";
import Report from "./page/Report.jsx";

function App() {
    return (
        <UserProvider>
            <BrowserRouter>
                <Routes>
                    <Route path={'/'} element={<Login />} />
                    <Route path={'/Main'} element={<Main />} />
                    <Route path={'/UserManagement'} element={<UserManagement />} />
                    <Route path={'/GroupManagement'} element={<GroupManagement />}/>
                    <Route path={"/InsPage/:id"} element={<InsPage />} />
                    <Route path={"/CompAnalysis/:sectionId"} element={<CompAnalysis />} />
                    <Route path={"/ProjectManagement"} element={<ProjectManagement />} />
                    <Route path={"/Report"} element={<Report />}/>
                </Routes>
            </BrowserRouter>
        </UserProvider>
    );
}

export default App
