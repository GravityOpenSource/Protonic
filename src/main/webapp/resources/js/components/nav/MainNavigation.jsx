import React from "react";
import { Avatar, Input, Menu } from "antd";
import { IconFolder, IconLogout, IconUser } from "../icons/Icons";
import { setBaseUrl } from "../../utilities/url-utilities";
import { grey1, grey4 } from "../../styles/colors";
import { SPACE_MD, SPACE_XS } from "../../styles/spacing";

const { Item, ItemGroup, SubMenu } = Menu;

export function MainNavigation({}) {
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        height: 47,
        backgroundColor: grey1,
        borderBottom: `1px solid rgb(240, 240, 240)`,
      }}
    >
      <a href={setBaseUrl("")}>
        <img
          alt="IRIDA Logo - link home"
          style={{ height: 30, padding: `0 ${SPACE_MD}` }}
          src={setBaseUrl("/resources/img/irida_logo_light.svg")}
        />
      </a>
      <form action={setBaseUrl("/search")}>
        <Input.Search name="query" style={{ width: 400 }} />
      </form>
      <Menu mode="horizontal" style={{ flexGrow: 1 }}>
        <SubMenu
          title={
            <>
              <IconFolder />
              {i18n("nav.main.project")}
            </>
          }
        >
          <Item key="user:projects">
            <a href={setBaseUrl(`/projects`)}>
              {i18n("nav.main.project-list")}
            </a>
          </Item>
          <Item key="user:connect">
            <a href={setBaseUrl(`/projects/synchronize`)}>
              {i18n("nav.main.project-sync")}
            </a>
          </Item>
          {window.TL._USER.systemRole === "ROLE_ADMIN" ? (
            <ItemGroup title="Admin">
              <Item key={"admin:projects"}>
                <a href={setBaseUrl(`/projects/all`)}>
                  {i18n("nav.main.project-list-all")}
                </a>
              </Item>
            </ItemGroup>
          ) : null}
        </SubMenu>
      </Menu>
      <Menu mode="horizontal">
        <SubMenu
          title={
            <>
              <Avatar
                size="small"
                style={{ backgroundColor: grey4, marginRight: SPACE_XS }}
                icon={<IconUser />}
              />
              {`${window.TL._USER.firstName} ${window.TL._USER.lastName}`}
            </>
          }
        >
          <Item>
            <>
              <IconLogout style={{ marginRight: SPACE_XS }} />
              <a href={setBaseUrl("/logout")}>{i18n("nav.main.logout")}</a>
            </>
          </Item>
        </SubMenu>
      </Menu>
    </div>
  );
}
